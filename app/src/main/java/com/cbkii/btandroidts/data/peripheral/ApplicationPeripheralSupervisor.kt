package com.cbkii.btandroidts.data.peripheral

import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.HidOperationResult
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicyStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralReconnectResult
import com.cbkii.btandroidts.domain.peripheral.PeripheralRetryState
import com.cbkii.btandroidts.domain.peripheral.PeripheralRetryStateAction
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisorState
import com.cbkii.btandroidts.domain.peripheral.ReconnectAttempt
import com.cbkii.btandroidts.domain.peripheral.ReconnectBackoff
import com.cbkii.btandroidts.domain.peripheral.ReconnectPolicy
import com.cbkii.btandroidts.domain.peripheral.SavedPeripheral
import com.cbkii.btandroidts.domain.peripheral.SavedPeripheralRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

class ApplicationPeripheralSupervisor(
	private val inventoryRepository: BluetoothDeviceInventoryRepository,
	private val policyStore: PeripheralPolicyStore,
	private val hidHostController: HidHostController,
) : PeripheralSupervisor {

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private val _state = MutableStateFlow(PeripheralSupervisorState())
	override val state: StateFlow<PeripheralSupervisorState> = _state.asStateFlow()

	init {
		scope.launch {
			policyStore.policy.collect { policy ->
				_state.value = PeripheralSupervisorState(
					enabled = policy.supervisionEnabled,
					safeModeEnabled = policy.safeModeEnabled,
					savedPeripherals = policy.savedPeripherals.map { saved ->
						SavedPeripheral(
							address = saved.address,
							displayName = saved.displayName,
							policy = saved.policy,
							savedAtMillis = saved.savedAtMillis,
						)
					},
					activeAttempts = policy.retryStates.map { (address, retry) ->
						ReconnectAttempt(
							address = address,
							attemptNumber = retry.attempt,
							nextAttemptAtMillis = retry.nextAttemptAtMillis,
							reason = retry.lastError ?: "Scheduled retry",
						)
					},
					lastEvent = _state.value.lastEvent,
				)
			}
		}
	}

	override suspend fun savePeripheral(address: BluetoothAddress, policy: ReconnectPolicy): Result<Unit> {
		val device = inventoryRepository.devices.value.firstOrNull { it.address == address }
			?: return Result.failure(IllegalArgumentException("Device $address is not in inventory"))
		if (device.isProtected) {
			return Result.failure(IllegalStateException("Protected devices cannot be supervised: ${device.protectionStatus}"))
		}
		policyStore.savePeripheral(
			SavedPeripheralRecord(
				address = address,
				displayName = device.displayName,
				policy = policy,
				savedAtMillis = System.currentTimeMillis(),
			)
		)
		_state.update { it.copy(lastEvent = "Saved ${device.displayName} for finite supervision") }
		return Result.success(Unit)
	}

	override suspend fun removeSavedPeripheral(address: BluetoothAddress): Result<Unit> {
		policyStore.removeSavedPeripheral(address)
		_state.update { it.copy(lastEvent = "Removed ${address.value} from supervision") }
		return Result.success(Unit)
	}

	override suspend fun setEnabled(enabled: Boolean) {
		policyStore.setSupervisionEnabled(enabled)
		_state.update { it.copy(lastEvent = if (enabled) "Supervisor enabled" else "Supervisor disabled") }
	}

	override suspend fun reconcile(reason: String): Result<Unit> {
		val policy = policyStore.currentPolicy()
		if (!policy.supervisionEnabled) {
			return Result.success(Unit)
		}
		if (policy.safeModeEnabled) {
			_state.update { it.copy(lastEvent = "Safe mode enabled; hidden reconnect operations skipped") }
			return Result.success(Unit)
		}

		inventoryRepository.stopScan()
		inventoryRepository.refreshBondedDevices()

		val now = System.currentTimeMillis()
		val reconnectResults = mutableListOf<PeripheralReconnectResult>()
		policy.savedPeripherals.forEach { saved ->
			val retry = policy.retryStates[saved.address]
			if (retry != null && retry.nextAttemptAtMillis > now) return@forEach
			if ((retry?.attempt ?: 0) >= saved.policy.maxAttempts) {
				reconnectResults += PeripheralReconnectResult(
					address = saved.address,
					result = "Retry limit reached during $reason",
					atMillis = now,
				)
				return@forEach
			}

			val result = hidHostController.connect(saved.address)
			val resultText = result.toResultText()
			val resultAt = System.currentTimeMillis()
			val retryStateAction = when (result) {
				HidOperationResult.Started,
				HidOperationResult.RequiresDeviceValidation -> PeripheralRetryStateAction.CLEAR
				else -> PeripheralRetryStateAction.SET
			}
			val retryState = when (result) {
				HidOperationResult.Started,
				HidOperationResult.RequiresDeviceValidation -> null
				else -> {
					val nextAttempt = (retry?.attempt ?: 0) + 1
					val delayMillis = ReconnectBackoff.nextDelayMillis(saved.policy, nextAttempt, saved.address)
					PeripheralRetryState(
						attempt = nextAttempt,
						nextAttemptAtMillis = resultAt + delayMillis,
						lastError = resultText,
					)
				}
			}
			reconnectResults += PeripheralReconnectResult(
				address = saved.address,
				result = resultText,
				atMillis = resultAt,
				retryStateAction = retryStateAction,
				retryState = retryState,
			)
		}
		if (reconnectResults.isNotEmpty()) {
			policyStore.applyReconnectResults(reconnectResults)
		}
		_state.update { it.copy(lastEvent = "Reconciled saved peripherals: $reason") }
		return Result.success(Unit)
	}

	private fun HidOperationResult.toResultText(): String = when (this) {
		HidOperationResult.Started -> "HID operation started"
		HidOperationResult.NotAvailable -> "HID host unavailable"
		HidOperationResult.RequiresPrivilege -> "HID host requires privileged install"
		HidOperationResult.RequiresDeviceValidation -> "HID result requires TS18 validation"
		is HidOperationResult.Failed -> "HID failed: $reason"
	}
}
