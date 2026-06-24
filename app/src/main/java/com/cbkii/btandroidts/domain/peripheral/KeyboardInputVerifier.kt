package com.cbkii.btandroidts.domain.peripheral

object KeyboardInputVerifier {
    fun shouldStartVerification(
        previousText: String,
        newText: String,
        alreadyAttempted: Boolean,
        verifiedAddress: BluetoothAddress?,
    ): Boolean = !alreadyAttempted && verifiedAddress == null && previousText.isEmpty() && newText.isNotEmpty()

    fun matchingBondedDevices(
        devices: List<UnifiedBluetoothDevice>,
        inputDevices: List<AndroidInputDeviceInfo>,
    ): List<UnifiedBluetoothDevice> = devices.filter { device ->
        device.bondState == BondStatus.BONDED &&
            inputDevices.any { inputDevice -> inputDevice.isKeyboard && inputDevice.matches(device.address) }
    }

    private fun AndroidInputDeviceInfo.matches(address: BluetoothAddress): Boolean {
        val compact = address.value.replace(":", "")
        return descriptor.contains(address.value, ignoreCase = true) ||
            descriptor.contains(compact, ignoreCase = true) ||
            name.contains(address.value, ignoreCase = true)
    }
}
