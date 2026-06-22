package com.cbkii.btandroidts.domain.peripheral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeripheralSupervisorPolicyTest {

	@Test
	fun reconnectBackoffIsFiniteAndBounded() {
		val address = BluetoothAddress.requireValid("12:34:56:78:9A:BC")
		val policy = ReconnectPolicy(
			maxAttempts = 3,
			initialDelayMillis = 1_000L,
			maxDelayMillis = 8_000L,
		)

		val delays = (0..5).map { attempt ->
			ReconnectBackoff.nextDelayMillis(policy, attempt, address)
		}

		assertTrue(delays.all { it in 1_000L..8_000L })
		assertEquals(8_000L, delays.last())
	}

	@Test(expected = IllegalArgumentException::class)
	fun reconnectPolicyRejectsUnboundedAttempts() {
		ReconnectPolicy(maxAttempts = 99)
	}

	@Test
	fun bluetoothAddressNormalizesAndRejectsInvalidMacs() {
		assertEquals("AA:BB:CC:DD:EE:FF", BluetoothAddress.requireValid("aa:bb:cc:dd:ee:ff").value)
		assertEquals(null, BluetoothAddress.parse("not-a-mac"))
	}
}
