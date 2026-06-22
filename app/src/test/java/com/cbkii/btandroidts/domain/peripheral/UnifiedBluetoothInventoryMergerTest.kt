package com.cbkii.btandroidts.domain.peripheral

import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothDeviceType
import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedBluetoothInventoryMergerTest {

	private val merger = UnifiedBluetoothInventoryMerger(staleAfterMillis = 1_000L)

	@Test
	fun mergeClassicAndBleResultsIntoOneDevice() {
		val classic = BluetoothDeviceModel(
			name = "Keyboard",
			address = "AA:BB:CC:DD:EE:FF",
			mode = BluetoothMode.BLUETOOTH_DEVICE_CLASSIC,
			type = BluetoothDeviceType.PERIPHERAL,
		)
		val ble = BluetoothLEDeviceModel(
			deviceModel = classic.copy(mode = BluetoothMode.BLUETOOTH_DEVICE_LE),
			deviceName = "Keyboard BLE",
			rssi = -52,
		)

		val inventory = merger.mergeBleDiscovered(
			current = merger.mergeClassicDiscovered(emptyList(), classic, rssi = null, nowMillis = 100L),
			device = ble,
			nowMillis = 200L,
		)

		assertEquals(1, inventory.size)
		assertEquals(setOf(DeviceTransport.CLASSIC, DeviceTransport.BLE), inventory.single().transports)
		assertEquals(BluetoothMode.BLUETOOTH_DEVICE_DUAL, inventory.single().mode)
		assertEquals(-52, inventory.single().rssi)
	}

	@Test
	fun bondedDevicesAreRetainedWhenStale() {
		val device = BluetoothDeviceModel(
			name = "SmartRemote",
			address = "DE:8F:7D:8E:A3:1E",
			mode = BluetoothMode.BLUETOOTH_DEVICE_LE,
			type = BluetoothDeviceType.PERIPHERAL,
		)

		val inventory = merger.mergeClassicBonded(emptyList(), listOf(device), nowMillis = 100L)
		val expired = merger.expireStaleDiscoveries(inventory, nowMillis = 10_000L)

		assertEquals(1, expired.size)
		assertEquals(BondStatus.BONDED, expired.single().bondState)
	}

	@Test
	fun staleUnbondedDiscoveriesExpire() {
		val device = BluetoothDeviceModel(
			name = "Temporary",
			address = "AA:BB:CC:DD:EE:01",
			mode = BluetoothMode.BLUETOOTH_DEVICE_CLASSIC,
			type = BluetoothDeviceType.PERIPHERAL,
		)

		val inventory = merger.mergeClassicDiscovered(emptyList(), device, rssi = null, nowMillis = 100L)
		val expired = merger.expireStaleDiscoveries(inventory, nowMillis = 10_000L)

		assertTrue(expired.isEmpty())
	}

	@Test
	fun topwayCarKitAddressIsProtectedAndOwnedByAutomotiveLane() {
		val device = BluetoothDeviceModel(
			name = "CarKit_blink",
			address = BluetoothProtectionPolicy.TOPWAY_CARKIT_ADDRESS,
			mode = BluetoothMode.BLUETOOTH_DEVICE_CLASSIC,
			type = BluetoothDeviceType.PHONE,
		)

		val inventory = merger.mergeClassicDiscovered(emptyList(), device, rssi = null, nowMillis = 100L)
		val protected = inventory.single()

		assertEquals(DeviceProtectionStatus.TS18_AUTOMOTIVE_CONTROLLER, protected.protectionStatus)
		assertEquals(BluetoothLaneOwner.TOPWAY_AUTOMOTIVE, protected.laneOwner)
		assertTrue(protected.isProtected)
	}
}
