package com.cbkii.btandroidts.domain.peripheral

import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode
import org.junit.Assert.assertTrue
import org.junit.Test

class TopwayLaneGuardTest {

	@Test
	fun blocksTopwayLaneOwnedDevice() {
		val guard = TopwayLaneGuard(FakeVendorPackageInspector(topwayPresent = true))
		val decision = guard.canAssignAndroidPeripheralLane(device(laneOwner = BluetoothLaneOwner.TOPWAY_AUTOMOTIVE))

		assertTrue(decision is TopwayLaneDecision.Blocked)
	}

	@Test
	fun warnsForPhoneProfileWhenTopwayBluetoothIsPresent() {
		val guard = TopwayLaneGuard(FakeVendorPackageInspector(topwayPresent = true))
		val decision = guard.canAssignAndroidPeripheralLane(
			device(profileStates = mapOf(BluetoothProfileRole.HFP to ProfileConnectionState.DISCONNECTED))
		)

		assertTrue(decision is TopwayLaneDecision.Warn)
	}

	@Test
	fun allowsUnprotectedPeripheralWhenNoVendorPhoneLaneIsObserved() {
		val guard = TopwayLaneGuard(FakeVendorPackageInspector(topwayPresent = false))
		val decision = guard.canAssignAndroidPeripheralLane(device())

		assertTrue(decision is TopwayLaneDecision.Allow)
	}

	private fun device(
		laneOwner: BluetoothLaneOwner = BluetoothLaneOwner.ANDROID_PERIPHERAL,
		protectionStatus: DeviceProtectionStatus = DeviceProtectionStatus.UNPROTECTED,
		profileStates: Map<BluetoothProfileRole, ProfileConnectionState> = emptyMap(),
	): UnifiedBluetoothDevice =
		UnifiedBluetoothDevice(
			address = BluetoothAddress.requireValid("AA:BB:CC:DD:EE:01"),
			displayName = "Keyboard",
			transports = setOf(DeviceTransport.CLASSIC),
			mode = BluetoothMode.BLUETOOTH_DEVICE_CLASSIC,
			deviceType = null,
			rssi = null,
			bondState = BondStatus.BONDED,
			uuids = emptySet(),
			profileStates = profileStates,
			aclConnectionState = AclConnectionState.DISCONNECTED,
			firstSeenAtMillis = 1L,
			lastSeenAtMillis = 1L,
			protectionStatus = protectionStatus,
			laneOwner = laneOwner,
		)

	private class FakeVendorPackageInspector(
		private val topwayPresent: Boolean,
	) : VendorPackageInspector {
		override fun inspect(): VendorPackageSnapshot =
			VendorPackageSnapshot(
				packages = listOf(
					VendorPackageStatus(
						packageName = "com.tw.bt",
						family = VendorPackageFamily.TOPWAY_BLUETOOTH,
						installed = topwayPresent,
						enabled = topwayPresent,
						source = EvidenceSource.OBSERVED,
					)
				)
			)
	}
}
