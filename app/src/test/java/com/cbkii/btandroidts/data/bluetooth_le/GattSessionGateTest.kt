package com.cbkii.btandroidts.data.bluetooth_le

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GattSessionGateTest {

	@Test
	fun emptyGateIsClaimedByFirstIdentityOnly() {
		val gate = GattSessionGate()
		val active = Any()

		assertTrue(gate.activate(active))
		assertTrue(gate.activate(active))
		assertTrue(gate.isActive(active))
		assertFalse(gate.activate(Any()))
	}

	@Test
	fun retiredIdentityCannotReclaimGateAfterReplacement() {
		val gate = GattSessionGate()
		val retired = Any()
		val replacement = Any()

		assertTrue(gate.activate(retired))
		gate.retire(retired)
		assertFalse(gate.activate(retired))
		assertTrue(gate.activate(replacement))
		assertFalse(gate.isActive(retired))
		assertTrue(gate.isActive(replacement))
	}

	@Test
	fun retiringUnknownIdentityDoesNotEvictActiveSession() {
		val gate = GattSessionGate()
		val active = Any()

		assertTrue(gate.activate(active))
		gate.retire(Any())
		assertTrue(gate.isActive(active))
	}
}
