package com.cbkii.btandroidts.domain.phone_keyboard

import kotlinx.coroutines.flow.StateFlow

interface PhoneKeyboardScanController {
    val candidates: StateFlow<List<PhoneKeyboardCandidate>>
    val isScanning: StateFlow<Boolean>

    suspend fun startScan()
    suspend fun stopScan()
    fun clearCandidates()
}
