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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

    private val cacheLock = Any()
    private val _currentCandidates = LinkedHashMap<String, PhoneKeyboardCandidate>()
    private val clearTrigger = MutableStateFlow(0L)
    private var cacheGeneration = 0L

    override val candidates: StateFlow<List<PhoneKeyboardCandidate>> = combine(
        inventoryRepository.devices,
        hidHostController.profileStates,
        tickerFlow,
        clearTrigger
    ) { unifiedDevices, hidStates, currentTime, currentClearTrigger ->
        val initialGeneration = synchronized(cacheLock) { cacheGeneration }

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

            // Check verification result safely via short timeout
            val result = withTimeoutOrNull(500L) {
                 inputDeviceRepository.getVerificationResult(address).first()
            }
            val isEventVerified = result?.success == true

            val inputVerificationState = when {
                 isEventVerified -> PhoneKeyboardInputVerificationState.EVENT_VERIFIED
                 hasInputNode -> PhoneKeyboardInputVerificationState.NODE_CREATED
                 else -> PhoneKeyboardInputVerificationState.NOT_VERIFIED
            }

            synchronized(cacheLock) {
                if (initialGeneration != cacheGeneration) {
                    // Generation changed during the suspend/combine process (e.g. clearCandidates was called)
                    // Discard stale working calculation and return the current fresh state.
                    return@combine _currentCandidates.values.toList()
                }

                if (_currentCandidates.containsKey(addressStr)) {
                    val existing = _currentCandidates[addressStr]!!
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
                    _currentCandidates[addressStr] = PhoneKeyboardPolicy.recomputeGuidance(updated)
                } else if (isFresh) {
                    _currentCandidates[addressStr] = PhoneKeyboardPolicy.mapToCandidate(evidence, isBonded, inputVerificationState, hidProfileState)
                }
            }
        }

        synchronized(cacheLock) {
            if (initialGeneration != cacheGeneration) {
                return@combine _currentCandidates.values.toList()
            }

            val iterator = _currentCandidates.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!PhoneKeyboardPolicy.shouldRetainCandidate(entry.value, currentTime)) {
                    iterator.remove()
                }
            }

            return@combine _currentCandidates.values
                .sortedByDescending { PhoneKeyboardPolicy.calculateConfidenceScore(it) }
                .toList()
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        synchronized(cacheLock) {
            cacheGeneration++
            _currentCandidates.clear()
        }
        clearTrigger.value = System.currentTimeMillis()
    }
}
