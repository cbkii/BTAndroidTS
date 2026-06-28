package com.cbkii.btandroidts.domain.phone_keyboard

import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.DeviceTransport
import com.cbkii.btandroidts.domain.peripheral.BluetoothProtectionPolicy
import com.cbkii.btandroidts.domain.peripheral.BluetoothLaneOwner

object PhoneKeyboardPolicy {
    private const val CANDIDATE_TTL_MILLIS = 30_000L

    fun shouldRetainCandidate(evidence: PhoneKeyboardScanEvidence, currentTimeMillis: Long): Boolean {
        // Retain if seen within TTL
        return (currentTimeMillis - evidence.timestampMillis) <= CANDIDATE_TTL_MILLIS
    }

    fun shouldRetainCandidate(candidate: PhoneKeyboardCandidate, currentTimeMillis: Long): Boolean {
        // Retain if seen within TTL
        return (currentTimeMillis - candidate.lastSeenMillis) <= CANDIDATE_TTL_MILLIS
    }

    fun mergeCandidates(existing: PhoneKeyboardCandidate, newEvidence: PhoneKeyboardScanEvidence, currentTimeMillis: Long): PhoneKeyboardCandidate {
        // Only merge if identity matches closely enough.
        // Don't merge random address BLE devices with unrelated Classic devices if identity is weak.
        val isDifferentAddressTypeAndDifferentTransport =
            existing.transport != newEvidence.transport &&
            (existing.addressType == AddressType.RANDOM || newEvidence.addressType == AddressType.RANDOM)

        val finalTransport = if (existing.transport == newEvidence.transport) {
            existing.transport
        } else if (!isDifferentAddressTypeAndDifferentTransport) {
            DeviceTransport.DUAL
        } else {
            newEvidence.transport
        }

        val allUuids = existing.serviceUuids + newEvidence.serviceUuids
        val hasHid = existing.hasHidService1812 || newEvidence.hasHidService1812 || allUuids.contains("00001812-0000-1000-8000-00805f9b34fb")

        val merged = existing.copy(
            lastSeenMillis = newEvidence.timestampMillis,
            seenCount = existing.seenCount + 1,
            transport = finalTransport,
            displayName = newEvidence.name ?: existing.displayName,
            rawAdvertisedName = newEvidence.rawAdvertisedName ?: existing.rawAdvertisedName,
            serviceUuids = allUuids,
            hasHidService1812 = hasHid,
            isConnectable = newEvidence.isConnectable ?: existing.isConnectable,
            lastRssi = newEvidence.rssi ?: existing.lastRssi,
            manufacturerDataPresent = existing.manufacturerDataPresent || newEvidence.manufacturerDataPresent,
            serviceDataPresent = existing.serviceDataPresent || newEvidence.serviceDataPresent,
            inputVerificationState = existing.inputVerificationState
        )
        return recomputeGuidance(merged)
    }

    fun recomputeGuidance(candidate: PhoneKeyboardCandidate): PhoneKeyboardCandidate {
        val recommendedAction = when {
             candidate.protectedTopwayRisk -> PhoneKeyboardUserGuidance.CONFLICT_WARNING
             candidate.isBonded && candidate.inputVerificationState == PhoneKeyboardInputVerificationState.NOT_VERIFIED -> PhoneKeyboardUserGuidance.VERIFY_INPUT_IN_TEST
             candidate.isBonded && candidate.inputVerificationState == PhoneKeyboardInputVerificationState.NODE_CREATED -> PhoneKeyboardUserGuidance.VERIFY_INPUT_IN_TEST
             candidate.isBonded && candidate.inputVerificationState == PhoneKeyboardInputVerificationState.EVENT_VERIFIED -> PhoneKeyboardUserGuidance.NO_ACTION_REQUIRED
             else -> PhoneKeyboardUserGuidance.OPEN_APP_ENABLE_ADVERTISING
        }
        return candidate.copy(recommendedAction = recommendedAction)
    }

    fun calculateConfidenceScore(candidate: PhoneKeyboardCandidate): Int {
        var score = 0
        if (candidate.isBonded) score += 50
        if (candidate.hasHidService1812) score += 30
        val name = candidate.displayName?.lowercase() ?: ""
        if (name.contains("keyboard") || name.contains("hid")) score += 20
        if (candidate.transport == DeviceTransport.CLASSIC) score += 10
        if (candidate.transport == DeviceTransport.BLE) score += 10
        if (candidate.transport == DeviceTransport.DUAL) score += 20
        return score
    }

    fun isTopwayProtected(address: BluetoothAddress, name: String?): Boolean {
        val policy = BluetoothProtectionPolicy()
        return policy.classify(address, name).laneOwner == BluetoothLaneOwner.TOPWAY_AUTOMOTIVE
    }

    fun mapToCandidate(
        evidence: PhoneKeyboardScanEvidence,
        isBonded: Boolean,
        inputVerificationState: PhoneKeyboardInputVerificationState,
        hidProfileState: com.cbkii.btandroidts.domain.peripheral.ProfileConnectionState
    ): PhoneKeyboardCandidate {
        val isProtected = isTopwayProtected(evidence.address, evidence.name)

        val candidate = PhoneKeyboardCandidate(
            candidateId = evidence.candidateId,
            address = evidence.address,
            transport = evidence.transport,
            firstSeenMillis = evidence.timestampMillis,
            lastSeenMillis = evidence.timestampMillis,
            seenCount = 1,
            displayName = evidence.name,
            rawAdvertisedName = evidence.rawAdvertisedName,
            scanRecordName = null,
            serviceUuids = evidence.serviceUuids,
            hasHidService1812 = evidence.hasHidService1812 || evidence.serviceUuids.contains("00001812-0000-1000-8000-00805f9b34fb"),
            manufacturerDataPresent = evidence.manufacturerDataPresent,
            serviceDataPresent = evidence.serviceDataPresent,
            isConnectable = evidence.isConnectable,
            addressType = evidence.addressType,
            isBonded = isBonded,
            protectedTopwayRisk = isProtected,
            lastRssi = evidence.rssi,
            hidProfileState = hidProfileState,
            inputVerificationState = inputVerificationState,
            recommendedAction = PhoneKeyboardUserGuidance.NO_ACTION_REQUIRED,
            lastFailureReason = null
        )
        return recomputeGuidance(candidate)
    }
}
