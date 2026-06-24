package com.cbkii.btandroidts.domain.peripheral

import com.cbkii.btandroidts.presentation.navigation.screens.KeyboardInputVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardInputVerifierTest {
    @Test
    fun emptyToNonEmptyTransitionTriggersOnce() {
        assertTrue(KeyboardInputVerifier.shouldStartVerification("", "a", alreadyAttempted = false, verifiedAddress = null))
        assertFalse(KeyboardInputVerifier.shouldStartVerification("a", "ab", alreadyAttempted = false, verifiedAddress = null))
        assertFalse(KeyboardInputVerifier.shouldStartVerification("", "a", alreadyAttempted = true, verifiedAddress = null))
        assertFalse(KeyboardInputVerifier.shouldStartVerification("", "a", alreadyAttempted = false, verifiedAddress = BluetoothAddress.requireValid("00:11:22:33:44:55")))
    }

    @Test
    fun noTargetDeviceDoesNotMatchAllBondedDevices() {
        val devices = listOf(device("00:11:22:33:44:55"), device("66:77:88:99:AA:BB"))
        val matches = KeyboardInputVerifier.matchingBondedDevices(devices, emptyList())
        assertEquals(emptyList<UnifiedBluetoothDevice>(), matches)
    }

    @Test
    fun ambiguousMatchingReturnsMultipleCandidatesForCallerToReject() {
        val first = device("00:11:22:33:44:55")
        val second = device("66:77:88:99:AA:BB")
        val inputs = listOf(
            input("bt-001122334455"),
            input("bt-66778899AABB"),
        )
        val matches = KeyboardInputVerifier.matchingBondedDevices(listOf(first, second), inputs)
        assertEquals(listOf(first, second), matches)
    }

    @Test
    fun matchingIgnoresUnbondedDevices() {
        val bonded = device("00:11:22:33:44:55")
        val unbonded = device("66:77:88:99:AA:BB", BondStatus.NONE)
        val inputs = listOf(input("66778899AABB"))
        assertEquals(emptyList<UnifiedBluetoothDevice>(), KeyboardInputVerifier.matchingBondedDevices(listOf(bonded, unbonded), inputs))
    }


    @Test
    fun matchingIgnoresNonKeyboardInputDevices() {
        val bonded = device("00:11:22:33:44:55")
        val inputs = listOf(input("001122334455", isKeyboard = false))
        assertEquals(emptyList<UnifiedBluetoothDevice>(), KeyboardInputVerifier.matchingBondedDevices(listOf(bonded), inputs))
    }

    private fun device(address: String, bondStatus: BondStatus = BondStatus.BONDED): UnifiedBluetoothDevice =
        UnifiedBluetoothDevice(
            address = BluetoothAddress.requireValid(address),
            displayName = address,
            transports = setOf(DeviceTransport.CLASSIC),
            mode = com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode.BLUETOOTH_DEVICE,
            deviceType = null,
            rssi = null,
            bondState = bondStatus,
            uuids = emptySet(),
            profileStates = emptyMap(),
            aclConnectionState = AclConnectionState.DISCONNECTED,
            firstSeenAtMillis = 0L,
            lastSeenAtMillis = 0L,
            protectionStatus = DeviceProtectionStatus.UNPROTECTED,
            laneOwner = BluetoothLaneOwner.ANDROID_PERIPHERAL,
        )

    private fun input(name: String, isKeyboard: Boolean = true): AndroidInputDeviceInfo = AndroidInputDeviceInfo(
        id = 1,
        name = name,
        descriptor = name,
        isKeyboard = isKeyboard,
        isPointer = false,
        sources = 0,
    )
}
