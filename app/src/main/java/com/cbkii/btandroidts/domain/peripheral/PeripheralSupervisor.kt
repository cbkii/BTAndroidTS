package com.cbkii.btandroidts.domain.peripheral

import kotlinx.coroutines.flow.StateFlow

interface PeripheralSupervisor {
	val state: StateFlow<PeripheralSupervisorState>

	suspend fun savePeripheral(address: BluetoothAddress, policy: ReconnectPolicy): Result<Unit>
	suspend fun removeSavedPeripheral(address: BluetoothAddress): Result<Unit>
	suspend fun setEnabled(enabled: Boolean)
	suspend fun reconcile(reason: String): Result<Unit>
}

interface PeripheralSupervisorScheduler {
	fun requestManualRetry(reason: String): Result<Unit>
	fun setSupervisionEnabled(enabled: Boolean, reason: String): Result<Unit>
}

data class PeripheralSupervisorState(
	val enabled: Boolean = false,
	val safeModeEnabled: Boolean = true,
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

object ReconnectBackoff {
	fun nextDelayMillis(
		policy: ReconnectPolicy,
		attemptNumber: Int,
		address: BluetoothAddress,
	): Long {
		val multiplier = 1L shl attemptNumber.coerceIn(0, 4)
		val base = (policy.initialDelayMillis * multiplier).coerceAtMost(policy.maxDelayMillis)
		val jitter = (address.value.hashCode().toLong() and Long.MAX_VALUE) % JITTER_WINDOW_MILLIS
		return (base + jitter).coerceAtMost(policy.maxDelayMillis)
	}

	private const val JITTER_WINDOW_MILLIS = 750L
}
