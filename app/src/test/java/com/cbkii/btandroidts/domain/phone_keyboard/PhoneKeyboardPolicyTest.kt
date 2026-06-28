package com.cbkii.btandroidts.domain.phone_keyboard

import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.DeviceTransport
import com.cbkii.btandroidts.domain.peripheral.ProfileConnectionState
import org.junit.Assert.*
import org.junit.Test

class PhoneKeyboardPolicyTest {

    @Test
    fun testShouldRetainCandidate() {
        val currentTime = 100_000L
        val evidence = PhoneKeyboardScanEvidence(
            candidateId = "00:11:22:33:44:55",
            address = BluetoothAddress.requireValid("00:11:22:33:44:55"),
            transport = DeviceTransport.BLE,
            name = "Test",
            rssi = -50,
            isConnectable = true,
            addressType = AddressType.PUBLIC,
            hasHidService1812 = true,
            serviceUuids = emptySet(),
            timestampMillis = currentTime - 10_000L
        )

        assertTrue(PhoneKeyboardPolicy.shouldRetainCandidate(evidence, currentTime))
        assertFalse(PhoneKeyboardPolicy.shouldRetainCandidate(evidence, currentTime + 40_000L))
    }

    @Test
    fun testMergeCandidates_SameTransport() {
        val currentTime = 100_000L
        val existing = PhoneKeyboardCandidate(
            candidateId = "00:11:22:33:44:55",
            address = BluetoothAddress.requireValid("00:11:22:33:44:55"),
            transport = DeviceTransport.BLE,
            firstSeenMillis = currentTime - 10_000L,
            lastSeenMillis = currentTime - 10_000L,
            seenCount = 1,
            displayName = null,
            rawAdvertisedName = null,
            scanRecordName = null,
            serviceUuids = emptySet(),
            hasHidService1812 = false,
            manufacturerDataPresent = false,
            serviceDataPresent = false,
            isConnectable = null,
            addressType = AddressType.PUBLIC,
            isBonded = false,
            protectedTopwayRisk = false,
            lastRssi = -80,
            hidProfileState = ProfileConnectionState.UNKNOWN,
            inputVerificationState = PhoneKeyboardInputVerificationState.NOT_VERIFIED,
            recommendedAction = PhoneKeyboardUserGuidance.NO_ACTION_REQUIRED,
            lastFailureReason = null
        )

        val newEvidence = PhoneKeyboardScanEvidence(
            candidateId = "00:11:22:33:44:55",
            address = BluetoothAddress.requireValid("00:11:22:33:44:55"),
            transport = DeviceTransport.BLE,
            name = "NewName",
            rssi = -50,
            isConnectable = false,
            addressType = AddressType.PUBLIC,
            hasHidService1812 = true,
            serviceUuids = setOf("00001812-0000-1000-8000-00805f9b34fb"),
            timestampMillis = currentTime
        )

        val merged = PhoneKeyboardPolicy.mergeCandidates(existing, newEvidence, currentTime)

        assertEquals("NewName", merged.displayName)
        assertEquals(DeviceTransport.BLE, merged.transport)
        assertEquals(2, merged.seenCount)
        assertEquals(currentTime, merged.lastSeenMillis)
        assertTrue(merged.hasHidService1812)
        assertEquals(false, merged.isConnectable)
        assertEquals(-50, merged.lastRssi)
    }

    @Test
    fun testMergeCandidates_DifferentTransport_RandomAddress() {
        val currentTime = 100_000L
        val existing = PhoneKeyboardCandidate(
            candidateId = "00:11:22:33:44:55",
            address = BluetoothAddress.requireValid("00:11:22:33:44:55"),
            transport = DeviceTransport.BLE,
            firstSeenMillis = currentTime - 10_000L,
            lastSeenMillis = currentTime - 10_000L,
            seenCount = 1,
            displayName = "DeviceA",
            rawAdvertisedName = null,
            scanRecordName = null,
            serviceUuids = emptySet(),
            hasHidService1812 = false,
            manufacturerDataPresent = false,
            serviceDataPresent = false,
            isConnectable = null,
            addressType = AddressType.RANDOM,
            isBonded = false,
            protectedTopwayRisk = false,
            lastRssi = -80,
            hidProfileState = ProfileConnectionState.UNKNOWN,
            inputVerificationState = PhoneKeyboardInputVerificationState.NOT_VERIFIED,
            recommendedAction = PhoneKeyboardUserGuidance.NO_ACTION_REQUIRED,
            lastFailureReason = null
        )

        val newEvidence = PhoneKeyboardScanEvidence(
            candidateId = "00:11:22:33:44:55",
            address = BluetoothAddress.requireValid("00:11:22:33:44:55"),
            transport = DeviceTransport.CLASSIC,
            name = "DeviceB",
            rssi = -50,
            isConnectable = true,
            addressType = AddressType.PUBLIC,
            hasHidService1812 = true,
            serviceUuids = emptySet(),
            timestampMillis = currentTime
        )

        val merged = PhoneKeyboardPolicy.mergeCandidates(existing, newEvidence, currentTime)

        // It shouldn't merge to DUAL because one is RANDOM and transports differ. It takes the new transport.
        assertEquals(DeviceTransport.CLASSIC, merged.transport)
        assertEquals("DeviceB", merged.displayName)
    }

    @Test
    fun testCalculateConfidenceScore_DUAL() {
        val existing = PhoneKeyboardCandidate(
            candidateId = "00:11:22:33:44:55",
            address = BluetoothAddress.requireValid("00:11:22:33:44:55"),
            transport = DeviceTransport.DUAL,
            firstSeenMillis = 0L,
            lastSeenMillis = 0L,
            seenCount = 1,
            displayName = "HID Keyboard",
            rawAdvertisedName = null,
            scanRecordName = null,
            serviceUuids = emptySet(),
            hasHidService1812 = true,
            manufacturerDataPresent = false,
            serviceDataPresent = false,
            isConnectable = null,
            addressType = AddressType.PUBLIC,
            isBonded = true,
            protectedTopwayRisk = false,
            lastRssi = -80,
            hidProfileState = ProfileConnectionState.UNKNOWN,
            inputVerificationState = PhoneKeyboardInputVerificationState.NOT_VERIFIED,
            recommendedAction = PhoneKeyboardUserGuidance.NO_ACTION_REQUIRED,
            lastFailureReason = null
        )
        // isBonded (50) + hasHid (30) + "keyboard"/"hid" in name (20) + DUAL (20) = 120
        assertEquals(120, PhoneKeyboardPolicy.calculateConfidenceScore(existing))
    }
}
