package com.cbkii.btandroidts.data.bluetooth_le

/**
 * Identity-based gate for callbacks emitted by successive BluetoothGatt objects.
 *
 * A newly observed token may claim an empty gate, which covers the documented possibility that a
 * connection callback arrives before connectGatt returns. Retired tokens remain rejected in a
 * small bounded history so late callbacks from recently closed sessions cannot claim the gate.
 */
internal class GattSessionGate(
	private val retiredCapacity: Int = DEFAULT_RETIRED_CAPACITY,
) {
	private val lock = Any()
	private val retiredTokens = ArrayDeque<Any>()
	private var activeToken: Any? = null

	init {
		require(retiredCapacity > 0) { "retiredCapacity must be positive" }
	}

	/** Activates [token] when the gate is empty, or confirms that it is already active. */
	fun activate(token: Any): Boolean = synchronized(lock) {
		if (retiredTokens.any { retired -> retired === token }) return@synchronized false

		val current = activeToken
		when {
			current == null -> {
				activeToken = token
				true
			}
			current === token -> true
			else -> false
		}
	}

	fun isActive(token: Any): Boolean = synchronized(lock) {
		activeToken === token
	}

	/** Retires [token] and retains its identity in a bounded late-callback rejection history. */
	fun retire(token: Any) = synchronized(lock) {
		if (activeToken === token) activeToken = null
		if (retiredTokens.none { retired -> retired === token }) {
			retiredTokens.addLast(token)
			while (retiredTokens.size > retiredCapacity) retiredTokens.removeFirst()
		}
	}

	private companion object {
		const val DEFAULT_RETIRED_CAPACITY = 8
	}
}
