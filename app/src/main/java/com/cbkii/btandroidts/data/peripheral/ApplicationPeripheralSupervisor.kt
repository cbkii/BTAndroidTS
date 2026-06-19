package com.cbkii.btandroidts.data.peripheral

import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisorState
import com.cbkii.btandroidts.domain.peripheral.ReconnectPolicy
import com.cbkii.btandroidts.domain.peripheral.SavedPeripheral
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ApplicationPeripheralSupervisor(
	private val inventoryRepository: BluetoothDeviceInventoryRepository,
) : PeripheralSupervisor {

	private val _state = MutableStateFlow(PeripheralSupervisorState())
	override val state: StateFlow<PeripheralSupervisorState> = _state.asStateFlow()

	override fun savePeripheral(address: BluetoothAddress, policy: ReconnectPolicy): Result<Unit> {
		val device = inventoryRepository.devices.value.firstOrNull { it.address == address }
			?: return Result.failure(IllegalArgumentException("Device $address is not in inventory"))
		if (device.isProtected) {
			return Result.failure(IllegalStateException("Protected devices cannot be supervised: ${device.protectionStatus}"))
		}
		_state.update { state ->
			state.copy(
				savedPeripherals = state.savedPeripherals
					.filterNot { it.address == address } + SavedPeripheral(
					address = address,
					displayName = device.displayName,
					policy = policy,
					savedAtMillis = System.currentTimeMillis(),
				),
				lastEvent = "Saved ${device.displayName} for finite supervision"
			)
		}
		return Result.success(Unit)
	}

	override fun removeSavedPeripheral(address: BluetoothAddress): Result<Unit> {
		_state.update { state ->
			state.copy(
				savedPeripherals = state.savedPeripherals.filterNot { it.address == address },
				activeAttempts = state.activeAttempts.filterNot { it.address == address },
				lastEvent = "Removed ${address.value} from supervision"
			)
		}
		return Result.success(Unit)
	}

	override fun setEnabled(enabled: Boolean) {
		_state.update {
			it.copy(
				enabled = enabled,
				activeAttempts = if (enabled) it.activeAttempts else emptyList(),
				lastEvent = if (enabled) "Supervisor enabled" else "Supervisor disabled"
			)
		}
	}
}
