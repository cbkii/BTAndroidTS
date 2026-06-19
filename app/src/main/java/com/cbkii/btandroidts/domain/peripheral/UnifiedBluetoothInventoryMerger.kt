package com.cbkii.btandroidts.domain.peripheral

import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel

class UnifiedBluetoothInventoryMerger(
	private val protectionPolicy: BluetoothProtectionPolicy = BluetoothProtectionPolicy(),
	private val staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
) {
	fun mergeClassicBonded(
		current: List<UnifiedBluetoothDevice>,
		devices: List<BluetoothDeviceModel>,
		nowMillis: Long,
	): List<UnifiedBluetoothDevice> =
		devices.fold(current) { inventory, device ->
			mergeClassicDevice(inventory, device, BondStatus.BONDED, null, nowMillis)
		}.sortedForDisplay()

	fun mergeClassicDiscovered(
		current: List<UnifiedBluetoothDevice>,
		device: BluetoothDeviceModel,
		rssi: Int?,
		nowMillis: Long,
	): List<UnifiedBluetoothDevice> =
		mergeClassicDevice(current, device, BondStatus.NONE, rssi, nowMillis).sortedForDisplay()

	fun mergeBleDiscovered(
		current: List<UnifiedBluetoothDevice>,
		device: BluetoothLEDeviceModel,
		nowMillis: Long,
	): List<UnifiedBluetoothDevice> {
		val base = device.deviceModel
		val address = BluetoothAddress.parse(base.address) ?: return current
		val existing = current.firstOrNull { it.address == address }
		val protection = protectionPolicy.classify(address, chooseName(base.name, device.deviceName))
		val merged = existing?.copy(
			displayName = chooseName(existing.displayName, device.deviceName, base.name),
			transports = existing.transports + DeviceTransport.BLE,
			mode = mergeMode(existing.mode, base.mode),
			deviceType = existing.deviceType ?: base.type,
			rssi = device.rssi,
			lastSeenAtMillis = nowMillis,
			protectionStatus = mostProtective(existing.protectionStatus, protection.status),
			laneOwner = mostSpecificLane(existing.laneOwner, protection.laneOwner),
		) ?: UnifiedBluetoothDevice(
			address = address,
			displayName = chooseName(device.deviceName, base.name),
			transports = setOf(DeviceTransport.BLE),
			mode = base.mode,
			deviceType = base.type,
			rssi = device.rssi,
			bondState = BondStatus.NONE,
			uuids = emptySet(),
			profileStates = emptyMap(),
			aclConnectionState = AclConnectionState.UNKNOWN,
			firstSeenAtMillis = nowMillis,
			lastSeenAtMillis = nowMillis,
			protectionStatus = protection.status,
			laneOwner = protection.laneOwner,
		)
		return current.replace(merged).sortedForDisplay()
	}

	fun expireStaleDiscoveries(
		current: List<UnifiedBluetoothDevice>,
		nowMillis: Long,
	): List<UnifiedBluetoothDevice> =
		current.filter { device ->
			device.bondState == BondStatus.BONDED ||
				device.isProtected ||
				nowMillis - device.lastSeenAtMillis <= staleAfterMillis
		}.sortedForDisplay()

	private fun mergeClassicDevice(
		current: List<UnifiedBluetoothDevice>,
		device: BluetoothDeviceModel,
		bondStatus: BondStatus,
		rssi: Int?,
		nowMillis: Long,
	): List<UnifiedBluetoothDevice> {
		val address = BluetoothAddress.parse(device.address) ?: return current
		val existing = current.firstOrNull { it.address == address }
		val protection = protectionPolicy.classify(address, device.name)
		val merged = existing?.copy(
			displayName = chooseName(existing.displayName, device.name),
			transports = existing.transports + DeviceTransport.CLASSIC,
			mode = mergeMode(existing.mode, device.mode),
			deviceType = existing.deviceType ?: device.type,
			rssi = rssi ?: existing.rssi,
			bondState = if (bondStatus == BondStatus.BONDED) BondStatus.BONDED else existing.bondState,
			lastSeenAtMillis = nowMillis,
			protectionStatus = mostProtective(existing.protectionStatus, protection.status),
			laneOwner = mostSpecificLane(existing.laneOwner, protection.laneOwner),
		) ?: UnifiedBluetoothDevice(
			address = address,
			displayName = chooseName(device.name),
			transports = setOf(DeviceTransport.CLASSIC),
			mode = device.mode,
			deviceType = device.type,
			rssi = rssi,
			bondState = bondStatus,
			uuids = emptySet(),
			profileStates = emptyMap(),
			aclConnectionState = AclConnectionState.UNKNOWN,
			firstSeenAtMillis = nowMillis,
			lastSeenAtMillis = nowMillis,
			protectionStatus = protection.status,
			laneOwner = protection.laneOwner,
		)
		return current.replace(merged)
	}

	private fun List<UnifiedBluetoothDevice>.replace(device: UnifiedBluetoothDevice): List<UnifiedBluetoothDevice> =
		filterNot { it.address == device.address } + device

	private fun List<UnifiedBluetoothDevice>.sortedForDisplay(): List<UnifiedBluetoothDevice> =
		sortedWith(compareByDescending<UnifiedBluetoothDevice> { it.bondState == BondStatus.BONDED }
			.thenByDescending { it.isProtected }
			.thenBy { it.displayName.lowercase() }
			.thenBy { it.address.value })

	private fun chooseName(vararg candidates: String?): String =
		candidates.firstOrNull { !it.isNullOrBlank() && it != BluetoothDeviceModel.UNNAMED_DEVICE_NAME }
			?: BluetoothDeviceModel.UNNAMED_DEVICE_NAME

	private fun mergeMode(existing: BluetoothMode, incoming: BluetoothMode): BluetoothMode =
		when {
			existing == incoming -> existing
			existing == BluetoothMode.BLUETOOTH_DEVICE_UNKNOWN -> incoming
			incoming == BluetoothMode.BLUETOOTH_DEVICE_UNKNOWN -> existing
			else -> BluetoothMode.BLUETOOTH_DEVICE_DUAL
		}

	private fun mostProtective(
		current: DeviceProtectionStatus,
		incoming: DeviceProtectionStatus,
	): DeviceProtectionStatus =
		if (PROTECTION_PRIORITY.getValue(incoming) > PROTECTION_PRIORITY.getValue(current)) incoming
		else current

	private fun mostSpecificLane(
		current: BluetoothLaneOwner,
		incoming: BluetoothLaneOwner,
	): BluetoothLaneOwner =
		when {
			current == BluetoothLaneOwner.TOPWAY_AUTOMOTIVE || incoming == BluetoothLaneOwner.TOPWAY_AUTOMOTIVE ->
				BluetoothLaneOwner.TOPWAY_AUTOMOTIVE
			current == BluetoothLaneOwner.ANDROID_PERIPHERAL || incoming == BluetoothLaneOwner.ANDROID_PERIPHERAL ->
				BluetoothLaneOwner.ANDROID_PERIPHERAL
			else -> BluetoothLaneOwner.UNKNOWN
		}

	companion object {
		const val DEFAULT_STALE_AFTER_MILLIS = 120_000L

		private val PROTECTION_PRIORITY = mapOf(
			DeviceProtectionStatus.UNPROTECTED to 0,
			DeviceProtectionStatus.USER_PROTECTED to 1,
			DeviceProtectionStatus.CRITICAL_SYSTEM_DEVICE to 2,
			DeviceProtectionStatus.TOPWAY_LANE_OWNED to 3,
			DeviceProtectionStatus.TS18_AUTOMOTIVE_CONTROLLER to 4,
		)
	}
}
