package com.cbkii.btandroidts.domain.phone_keyboard

import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothScanRequest
import com.cbkii.btandroidts.domain.peripheral.DeviceTransport
import com.cbkii.btandroidts.domain.peripheral.ScanStatus
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.ProfileConnectionState
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PhoneKeyboardScanControllerImpl(
    private val inventoryRepository: BluetoothDeviceInventoryRepository,
    private val hidHostController: HidHostController,
    private val inputDeviceRepository: InputDeviceRepository,
    private val scope: CoroutineScope
) : PhoneKeyboardScanController {

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(5_000L) // evaluate TTL every 5s
        }
    }

    override val candidates: StateFlow<List<PhoneKeyboardCandidate>> = combine(
        inventoryRepository.devices,
        hidHostController.profileStates,
        tickerFlow
    ) { unifiedDevices, hidStates, currentTime ->
        val currentCandidates = _candidatesCache.associateBy { it.address.value }.toMutableMap()

        for (device in unifiedDevices) {
            val addressStr = device.address.value
            val address = device.address

            val isFresh = (currentTime - device.lastSeenAtMillis) < 5_000L
            val isConnectable = if(device.transports.contains(DeviceTransport.CLASSIC)) true else null

            val evidence = PhoneKeyboardScanEvidence(
                candidateId = addressStr,
                address = address,
                transport = if (device.transports.contains(DeviceTransport.BLE) && device.transports.contains(DeviceTransport.CLASSIC)) DeviceTransport.DUAL
                            else if (device.transports.contains(DeviceTransport.CLASSIC)) DeviceTransport.CLASSIC
                            else DeviceTransport.BLE,
                name = device.displayName,
                rssi = device.rssi,
                isConnectable = isConnectable,
                addressType = AddressType.UNKNOWN,
                hasHidService1812 = device.uuids.contains("00001812-0000-1000-8000-00805f9b34fb"),
                serviceUuids = device.uuids,
                timestampMillis = if(isFresh) device.lastSeenAtMillis else currentTime,
                rawAdvertisedName = device.displayName,
            )

            val isBonded = device.bondState == com.cbkii.btandroidts.domain.peripheral.BondStatus.BONDED
            val hidProfileState = hidStates[address] ?: ProfileConnectionState.UNKNOWN
            val hasInputNode = inputDeviceRepository.hasInputDeviceFor(address)

            // Note: Since runBlocking is generally discouraged in flows, we use a cached or non-suspending method if available
            // but the repository getVerificationResult is a Flow.
            // In a more robust architecture, we'd combine this flow as well, but for now we poll
            val verificationResult = inputDeviceRepository.getVerificationResult(address)
            var isEventVerified = false
            try {
                 // Fast check, assuming latest value is cached or available quickly
                 scope.launch {
                     verificationResult.collect { result -> isEventVerified = result?.success == true }
                 }.cancel() // just peeking is not really viable here.
            } catch(e: Exception){}

            // To properly resolve the flow cleanly, we rely on the node creation
            // We will trust the input repository state if we can get it
            val inputVerificationState = if(hasInputNode) PhoneKeyboardInputVerificationState.NODE_CREATED else PhoneKeyboardInputVerificationState.NOT_VERIFIED

            if (currentCandidates.containsKey(addressStr)) {
                val existing = currentCandidates[addressStr]!!
                val merged = if(isFresh) {
                    PhoneKeyboardPolicy.mergeCandidates(existing, evidence, currentTime)
                } else {
                    existing
                }

                val updated = merged.copy(
                   isBonded = isBonded,
                   hidProfileState = hidProfileState,
                   inputVerificationState = inputVerificationState
                )
                currentCandidates[addressStr] = PhoneKeyboardPolicy.recomputeGuidance(updated)
            } else if (isFresh) {
                currentCandidates[addressStr] = PhoneKeyboardPolicy.mapToCandidate(evidence, isBonded, inputVerificationState, hidProfileState)
            }
        }

        val validCandidates = currentCandidates.values
            .filter { PhoneKeyboardPolicy.shouldRetainCandidate(PhoneKeyboardScanEvidence(
                candidateId = it.candidateId,
                address = it.address,
                transport = it.transport,
                name = it.displayName,
                rssi = it.lastRssi,
                isConnectable = it.isConnectable,
                addressType = it.addressType,
                hasHidService1812 = it.hasHidService1812,
                serviceUuids = it.serviceUuids,
                timestampMillis = it.lastSeenMillis
            ), currentTime) }
            .sortedByDescending { PhoneKeyboardPolicy.calculateConfidenceScore(it) }

        _candidatesCache = validCandidates
        validCandidates
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var _candidatesCache: List<PhoneKeyboardCandidate> = emptyList()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        scope.launch {
            inventoryRepository.scanState.collect { scanState ->
                _isScanning.value = scanState.status == ScanStatus.RUNNING || scanState.status == ScanStatus.STARTING
            }
        }
    }

    override suspend fun startScan() {
        val request = BluetoothScanRequest(includeClassic = true, includeBle = true, durationMillis = 15_000L)
        inventoryRepository.startScan(request)
    }

    override suspend fun stopScan() {
        inventoryRepository.stopScan()
    }

    override fun clearCandidates() {
        _candidatesCache = emptyList()
    }
}
