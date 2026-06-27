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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PhoneKeyboardScanControllerImpl(
    private val inventoryRepository: BluetoothDeviceInventoryRepository,
    private val hidHostController: HidHostController,
    private val inputDeviceRepository: InputDeviceRepository,
    private val scope: CoroutineScope
) : PhoneKeyboardScanController {

    private val _candidates = MutableStateFlow<List<PhoneKeyboardCandidate>>(emptyList())
    override val candidates: StateFlow<List<PhoneKeyboardCandidate>> = _candidates.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        scope.launch {
            inventoryRepository.scanState.collect { scanState ->
                _isScanning.value = scanState.status == ScanStatus.RUNNING || scanState.status == ScanStatus.STARTING
            }
        }

        scope.launch {
            combine(
                inventoryRepository.devices,
                hidHostController.profileStates
            ) { unifiedDevices, hidStates ->
                val currentTime = System.currentTimeMillis()

                val currentCandidates = _candidates.value.associateBy { it.address.value }.toMutableMap()

                for (device in unifiedDevices) {
                    val addressStr = device.address.value
                    val address = device.address

                    val isConnectable = if(device.transports.contains(DeviceTransport.CLASSIC)) true else null

                    val evidence = PhoneKeyboardScanEvidence(
                        candidateId = addressStr,
                        address = address,
                        transport = if (device.transports.contains(DeviceTransport.BLE)) DeviceTransport.BLE else DeviceTransport.CLASSIC,
                        name = device.displayName,
                        rssi = device.rssi,
                        isConnectable = isConnectable, // Don't hide non-connectable, preserve missing
                        addressType = AddressType.UNKNOWN, // Keep unknown as default since we don't know without deeper BLE inspection, but we preserve randoms by not filtering them out entirely.
                        hasHidService1812 = device.uuids.contains("00001812-0000-1000-8000-00805f9b34fb"),
                        serviceUuids = device.uuids,
                        timestampMillis = currentTime,
                        rawAdvertisedName = device.displayName,
                    )

                    val isBonded = device.bondState == com.cbkii.btandroidts.domain.peripheral.BondStatus.BONDED

                    val hidProfileState = hidStates[address]?.name ?: "UNKNOWN"
                    val hasInputNode = inputDeviceRepository.hasInputDeviceFor(address)
                    val inputVerificationState = if(hasInputNode) "Input Node Created" else "No Input Node"

                    val newCandidate = PhoneKeyboardPolicy.mapToCandidate(evidence, isBonded, inputVerificationState, hidProfileState)

                    if (currentCandidates.containsKey(addressStr)) {
                        val existing = currentCandidates[addressStr]!!
                        val merged = PhoneKeyboardPolicy.mergeCandidates(existing, evidence, currentTime)
                        // update states
                        currentCandidates[addressStr] = merged.copy(
                           isBonded = isBonded,
                           hidProfileState = hidProfileState,
                           inputVerificationState = inputVerificationState
                        )
                    } else {
                        currentCandidates[addressStr] = newCandidate
                    }
                }

                // apply TTL and ranking
                val validCandidates = currentCandidates.values
                    .filter { currentTime - it.lastSeenMillis <= 30_000L }
                    .sortedByDescending { PhoneKeyboardPolicy.calculateConfidenceScore(it) }

                _candidates.value = validCandidates
            }.collect {}
        }
    }

    override suspend fun startScan() {
        // Stage 1: BLE only (fast passes)
        val request = BluetoothScanRequest(includeClassic = false, includeBle = true, durationMillis = 10_000L)
        inventoryRepository.startScan(request)
    }

    override suspend fun stopScan() {
        inventoryRepository.stopScan()
    }

    override fun clearCandidates() {
        _candidates.value = emptyList()
    }
}
