package com.cbkii.btandroidts.domain.peripheral

import kotlinx.coroutines.flow.Flow

interface PeripheralPolicyStore {
	val policy: Flow<PeripheralPolicy>

	suspend fun currentPolicy(): PeripheralPolicy
	suspend fun setSupervisionEnabled(enabled: Boolean)
	suspend fun setSafeModeEnabled(enabled: Boolean)
	suspend fun savePeripheral(device: SavedPeripheralRecord)
	suspend fun removeSavedPeripheral(address: BluetoothAddress)
	suspend fun protectDevice(device: ProtectedPeripheralRecord)
	suspend fun removeProtectedDevice(address: BluetoothAddress)
	suspend fun setRetryState(address: BluetoothAddress, retryState: PeripheralRetryState?)
	suspend fun recordResult(address: BluetoothAddress, result: String, atMillis: Long)
	suspend fun applyReconnectResults(results: List<PeripheralReconnectResult>)
}

data class PeripheralPolicy(
	val supervisionEnabled: Boolean = false,
	val safeModeEnabled: Boolean = true,
	val savedPeripherals: List<SavedPeripheralRecord> = emptyList(),
	val protectedDevices: List<ProtectedPeripheralRecord> = emptyList(),
	val retryStates: Map<BluetoothAddress, PeripheralRetryState> = emptyMap(),
)

data class SavedPeripheralRecord(
	val address: BluetoothAddress,
	val displayName: String,
	val policy: ReconnectPolicy,
	val savedAtMillis: Long,
	val lastResult: String? = null,
	val lastResultAtMillis: Long? = null,
	val expertOverride: Boolean = false,
	val protectionReason: String? = null,
)

data class ProtectedPeripheralRecord(
	val address: BluetoothAddress,
	val displayName: String,
	val reason: String,
	val expertOverride: Boolean = false,
	val updatedAtMillis: Long,
)

data class PeripheralRetryState(
	val attempt: Int,
	val nextAttemptAtMillis: Long,
	val lastError: String? = null,
)

data class PeripheralReconnectResult(
	val address: BluetoothAddress,
	val result: String,
	val atMillis: Long,
	val retryStateAction: PeripheralRetryStateAction = PeripheralRetryStateAction.UNCHANGED,
	val retryState: PeripheralRetryState? = null,
) {
	init {
		require(retryStateAction != PeripheralRetryStateAction.SET || retryState != null) {
			"retryState is required when retryStateAction is SET"
		}
	}
}

enum class PeripheralRetryStateAction {
	UNCHANGED,
	CLEAR,
	SET,
}
