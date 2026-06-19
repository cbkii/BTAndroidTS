package com.cbkii.btandroidts.domain.peripheral

import kotlinx.coroutines.flow.StateFlow

interface PeripheralSupervisor {
	val state: StateFlow<PeripheralSupervisorState>

	fun savePeripheral(address: BluetoothAddress, policy: ReconnectPolicy): Result<Unit>
	fun removeSavedPeripheral(address: BluetoothAddress): Result<Unit>
	fun setEnabled(enabled: Boolean)
}

data class PeripheralSupervisorState(
	val enabled: Boolean = false,
	val savedPeripherals: List<SavedPeripheral> = emptyList(),
	val activeAttempts: List<ReconnectAttempt> = emptyList(),
	val lastEvent: String? = null,
)

data class SavedPeripheral(
	val address: BluetoothAddress,
	val displayName: String,
	val policy: ReconnectPolicy,
	val savedAtMillis: Long,
)

data class ReconnectPolicy(
	val maxAttempts: Int = 3,
	val initialDelayMillis: Long = 5_000L,
	val maxDelayMillis: Long = 60_000L,
) {
	init {
		require(maxAttempts in 0..5) { "Reconnect attempts must be between 0 and 5" }
		require(initialDelayMillis in 1_000L..maxDelayMillis)
		require(maxDelayMillis <= 120_000L)
	}
}

data class ReconnectAttempt(
	val address: BluetoothAddress,
	val attemptNumber: Int,
	val nextAttemptAtMillis: Long,
	val reason: String,
)
