package com.cbkii.btandroidts.domain.peripheral

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityRegistryTest {

	@Test
	fun ts18BaselineKeepsHidAndOppAsDeviceValidationItems() {
		val registry = CapabilityRegistry.ts18Baseline(
			hasBluetooth = true,
			hasBle = true,
			hasPrivilegedPermission = false,
			hasRoot = true,
		)

		assertEquals(CapabilityStatus.AVAILABLE, registry.get(PeripheralFeature.CLASSIC_SCAN).status)
		assertEquals(CapabilityStatus.AVAILABLE, registry.get(PeripheralFeature.BLE_SCAN).status)
		assertEquals(CapabilityStatus.REQUIRES_DEVICE_VALIDATION, registry.get(PeripheralFeature.HID_HOST).status)
		assertEquals(CapabilityStatus.REQUIRES_DEVICE_VALIDATION, registry.get(PeripheralFeature.OPP_SHARE).status)
		assertEquals(CapabilityStatus.REQUIRES_PRIVILEGE, registry.get(PeripheralFeature.SELECTIVE_UNPAIR).status)
	}
}
