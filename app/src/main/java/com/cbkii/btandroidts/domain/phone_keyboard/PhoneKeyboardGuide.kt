package com.cbkii.btandroidts.domain.phone_keyboard

object PhoneKeyboardGuide {

    fun getGuidanceText(reason: PhoneKeyboardFailureReason): String = when (reason) {
        PhoneKeyboardFailureReason.PHONE_APP_NOT_ADVERTISING ->
            "The phone app is not advertising. Open the app, enable Bluetooth keyboard/server/HID mode, and keep it foreground."
        PhoneKeyboardFailureReason.BLE_SEEN_NOT_CONNECTABLE ->
            "Device seen but not connectable. Ensure the app is foreground and not already connected to another host."
        PhoneKeyboardFailureReason.PAIRING_TIMEOUT ->
            "Pairing timed out. Keep the phone screen on and accept the pairing prompt quickly."
        PhoneKeyboardFailureReason.PAIRING_REJECTED_BY_PHONE ->
            "Pairing was rejected by the phone. Remove stale pairing for this TS18 on the phone and retry."
        PhoneKeyboardFailureReason.BOND_CREATED_NO_HID_SERVICE ->
            "Bonded, but no HID service found. The sender app may not be registered. Open the app and try again."
        PhoneKeyboardFailureReason.HID_SERVICE_SEEN_CONNECT_FAILED ->
            "HID service seen but connection failed. The phone may be denying the profile connection."
        PhoneKeyboardFailureReason.HID_CONNECTED_NO_INPUT_DEVICE ->
            "HID connected but Android did not create an input device. The Android stack might require a restart."
        PhoneKeyboardFailureReason.INPUT_DEVICE_ACTIVE_NO_KEY_EVENTS ->
            "Input device created but no key events received. Verify the app is sending valid reports."
        PhoneKeyboardFailureReason.TOPWAY_CONFLICT_RISK ->
            "This is a protected vendor/Topway device. Connecting it as a keyboard may cause system conflicts."
        PhoneKeyboardFailureReason.PRIVILEGED_HID_HOST_REQUIRED ->
            "Privileged HID Host access is required but unavailable. Ensure the Magisk module is installed and enabled."
        PhoneKeyboardFailureReason.UNSUPPORTED_TS18_STACK ->
            "The TS18 stack does not support this operation."
    }

    val senderAppCompatibilityGuide = listOf(
        SenderAppCompatibility(
            name = "Generic Android Bluetooth Keyboard app",
            expectedTransport = "BLE/HOGP or Classic HID",
            setupInstructions = "Open app, enable Bluetooth keyboard/server/HID mode, keep foreground, pair from TS18."
        ),
        SenderAppCompatibility(
            name = "Android app using BluetoothHidDevice (e.g. Kontroller, USBtoBLHid)",
            expectedTransport = "Classic HID (via Android HID Device profile)",
            setupInstructions = "Sender app must be registered. It may unregister if not foreground or if another HID app is registered."
        )
    )
}

data class SenderAppCompatibility(
    val name: String,
    val expectedTransport: String,
    val setupInstructions: String
)
