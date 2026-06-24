package com.cbkii.btandroidts.domain.peripheral

object KeyboardInputVerifier {
    fun shouldStartVerification(old: String, new: String, alreadyAttempted: Boolean, verifiedAddress: BluetoothAddress?): Boolean {
        return old.isEmpty() && new.isNotEmpty() && !alreadyAttempted && verifiedAddress == null
    }

    fun matchingBondedDevices(bondedDevices: List<UnifiedBluetoothDevice>, inputs: List<AndroidInputDeviceInfo>): List<UnifiedBluetoothDevice> {
        val keyboardInputs = inputs.filter { it.isKeyboard }
        return bondedDevices.filter { dev ->
            dev.bondState == BondStatus.BONDED && keyboardInputs.any { input ->
                val cleanAddr = dev.address.value.replace(":", "").uppercase()
                input.name.contains(cleanAddr, ignoreCase = true) || input.descriptor.contains(cleanAddr, ignoreCase = true)
            }
        }
    }
}
