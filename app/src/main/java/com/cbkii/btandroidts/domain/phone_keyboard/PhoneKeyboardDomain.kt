package com.cbkii.btandroidts.domain.phone_keyboard

import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.DeviceTransport

data class PhoneKeyboardCandidate(
    val candidateId: String,
    val address: BluetoothAddress,
    val transport: DeviceTransport,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
    val seenCount: Int,
    val displayName: String?,
    val rawAdvertisedName: String?,
    val scanRecordName: String?,
    val serviceUuids: Set<String>,
    val hasHidService1812: Boolean,
    val manufacturerDataPresent: Boolean,
    val serviceDataPresent: Boolean,
    val isConnectable: Boolean?,
    val addressType: AddressType,
    val isBonded: Boolean,
    val protectedTopwayRisk: Boolean,
    val lastRssi: Int?,
    val hidProfileState: com.cbkii.btandroidts.domain.peripheral.ProfileConnectionState,
    val inputVerificationState: PhoneKeyboardInputVerificationState,
    val recommendedAction: PhoneKeyboardUserGuidance,
    val lastFailureReason: PhoneKeyboardFailureReason?
)

enum class AddressType {
    PUBLIC, RANDOM, UNKNOWN
}

enum class PhoneKeyboardFailureReason {
    PHONE_APP_NOT_ADVERTISING,
    BLE_SEEN_NOT_CONNECTABLE,
    PAIRING_TIMEOUT,
    PAIRING_REJECTED_BY_PHONE,
    BOND_CREATED_NO_HID_SERVICE,
    HID_SERVICE_SEEN_CONNECT_FAILED,
    HID_CONNECTED_NO_INPUT_DEVICE,
    INPUT_DEVICE_ACTIVE_NO_KEY_EVENTS,
    TOPWAY_CONFLICT_RISK,
    PRIVILEGED_HID_HOST_REQUIRED,
    UNSUPPORTED_TS18_STACK
}

enum class PhoneKeyboardUserGuidance {
    OPEN_APP_ENABLE_ADVERTISING,
    PAIR_FROM_HOST,
    RETRY_PAIRING,
    VERIFY_INPUT_IN_TEST,
    REMOVE_STALE_PAIRING_RETRY,
    NO_ACTION_REQUIRED,
    CONFLICT_WARNING,
    CONNECTING
}

data class PhoneKeyboardScanEvidence(
    val candidateId: String,
    val address: BluetoothAddress,
    val transport: DeviceTransport,
    val name: String?,
    val rssi: Int?,
    val isConnectable: Boolean?,
    val addressType: AddressType,
    val hasHidService1812: Boolean,
    val serviceUuids: Set<String>,
    val timestampMillis: Long,
    val rawAdvertisedName: String? = null,
    val manufacturerDataPresent: Boolean = false,
    val serviceDataPresent: Boolean = false
)

enum class PhoneKeyboardInputVerificationState {
    NOT_VERIFIED,
    NODE_CREATED,
    EVENT_VERIFIED,
    FAILED
}
