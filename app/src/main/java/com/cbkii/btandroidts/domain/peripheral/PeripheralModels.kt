package com.cbkii.btandroidts.domain.peripheral

import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothDeviceType
import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode

data class UnifiedBluetoothDevice(
	val address: BluetoothAddress,
	val displayName: String,
	val transports: Set<DeviceTransport>,
	val mode: BluetoothMode,
	val deviceType: BluetoothDeviceType?,
	val rssi: Int?,
	val bondState: BondStatus,
	val uuids: Set<String>,
	val profileStates: Map<BluetoothProfileRole, ProfileConnectionState>,
	val aclConnectionState: AclConnectionState,
	val firstSeenAtMillis: Long,
	val lastSeenAtMillis: Long,
	val protectionStatus: DeviceProtectionStatus,
	val laneOwner: BluetoothLaneOwner,
) {
	val isProtected: Boolean
		get() = protectionStatus != DeviceProtectionStatus.UNPROTECTED ||
			laneOwner == BluetoothLaneOwner.TOPWAY_AUTOMOTIVE
}

enum class DeviceTransport {
	CLASSIC,
	BLE,
	DUAL,
	UNKNOWN,
}

enum class BondStatus {
	NONE,
	BONDING,
	BONDED,
	UNKNOWN,
}

enum class AclConnectionState {
	DISCONNECTED,
	CONNECTING,
	CONNECTED,
	DISCONNECTING,
	UNKNOWN,
}

enum class BluetoothProfileRole {
	RFCOMM_TERMINAL,
	BLE_GATT,
	HID_HOST,
	OPP,
	PAN,
	A2DP,
	HFP,
	AVRCP,
	PBAP,
	MAP,
}

enum class ProfileConnectionState {
	DISCONNECTED,
	CONNECTING,
	CONNECTED,
	DISCONNECTING,
	FAILED,
	UNKNOWN,
	REQUIRES_DEVICE_VALIDATION,
}

enum class DeviceProtectionStatus {
	UNPROTECTED,
	USER_PROTECTED,
	TS18_AUTOMOTIVE_CONTROLLER,
	TOPWAY_LANE_OWNED,
	CRITICAL_SYSTEM_DEVICE,
}

enum class BluetoothLaneOwner {
	ANDROID_PERIPHERAL,
	TOPWAY_AUTOMOTIVE,
	UNKNOWN,
}

data class BluetoothScanRequest(
	val includeClassic: Boolean = true,
	val includeBle: Boolean = true,
	val durationMillis: Long = DEFAULT_SCAN_DURATION_MILLIS,
) {
	init {
		require(includeClassic || includeBle) { "At least one Bluetooth transport must be scanned" }
		require(durationMillis in MIN_SCAN_DURATION_MILLIS..MAX_SCAN_DURATION_MILLIS) {
			"Scan duration must be between $MIN_SCAN_DURATION_MILLIS and $MAX_SCAN_DURATION_MILLIS ms"
		}
	}

	companion object {
		const val MIN_SCAN_DURATION_MILLIS = 5_000L
		const val DEFAULT_SCAN_DURATION_MILLIS = 15_000L
		const val MAX_SCAN_DURATION_MILLIS = 60_000L
	}
}

data class BoundedScanState(
	val status: ScanStatus = ScanStatus.IDLE,
	val startedAtMillis: Long? = null,
	val endsAtMillis: Long? = null,
	val activeTransports: Set<DeviceTransport> = emptySet(),
	val lastError: String? = null,
)

enum class ScanStatus {
	IDLE,
	STARTING,
	RUNNING,
	STOPPING,
	FAILED,
}

sealed interface BondingResult {
	data object AlreadyBonded : BondingResult
	data object Bonded : BondingResult
	data object Removed : BondingResult
	data class Failed(val reason: BondFailureReason, val detail: String? = null) : BondingResult
}

enum class BondFailureReason {
	INVALID_ADDRESS,
	PROTECTED_DEVICE,
	PERMISSION_MISSING,
	ADAPTER_UNAVAILABLE,
	START_FAILED,
	TIMEOUT,
	REMOTE_REJECTED,
	UNKNOWN,
}
