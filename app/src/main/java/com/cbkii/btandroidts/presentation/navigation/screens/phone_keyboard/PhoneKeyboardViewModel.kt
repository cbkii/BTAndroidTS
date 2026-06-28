package com.cbkii.btandroidts.presentation.navigation.screens.phone_keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardCandidate
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardScanController
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardFailureReason
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import com.cbkii.btandroidts.domain.peripheral.BondingResult
import com.cbkii.btandroidts.domain.peripheral.BondFailureReason
import com.cbkii.btandroidts.domain.peripheral.HidOperationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PhoneKeyboardViewModel(
    private val scanController: PhoneKeyboardScanController,
    private val bondController: BluetoothBondController,
    private val hidHostController: HidHostController,
    private val inputDeviceRepository: InputDeviceRepository
) : ViewModel() {

    val candidates = scanController.candidates
    val isScanning = scanController.isScanning

    private val _uiEvents = MutableSharedFlow<PhoneKeyboardUiEvent>()
    val uiEvents: SharedFlow<PhoneKeyboardUiEvent> = _uiEvents.asSharedFlow()

    fun startScan() {
        viewModelScope.launch {
            scanController.startScan()
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            scanController.stopScan()
        }
    }

    fun pairAndConnect(candidate: PhoneKeyboardCandidate) {
        viewModelScope.launch {
            try {
                var canConnectHid = candidate.isBonded

                if (!candidate.isBonded) {
                    val bondResult = bondController.createBond(candidate.address)
                    canConnectHid = when (bondResult) {
                        is BondingResult.AlreadyBonded, is BondingResult.Bonded -> true
                        is BondingResult.Failed -> {
                            val reason = when (bondResult.reason) {
                                BondFailureReason.TIMEOUT -> PhoneKeyboardFailureReason.PAIRING_TIMEOUT
                                BondFailureReason.REMOTE_REJECTED, BondFailureReason.START_FAILED -> PhoneKeyboardFailureReason.PAIRING_REJECTED_BY_PHONE
                                BondFailureReason.PROTECTED_DEVICE -> PhoneKeyboardFailureReason.TOPWAY_CONFLICT_RISK
                                BondFailureReason.PERMISSION_MISSING -> PhoneKeyboardFailureReason.PRIVILEGED_HID_HOST_REQUIRED
                                else -> PhoneKeyboardFailureReason.UNSUPPORTED_TS18_STACK
                            }
                            _uiEvents.emit(PhoneKeyboardUiEvent.ShowError(reason))
                            false
                        }
                        else -> false
                    }
                }

                if (canConnectHid) {
                    val hidResult = hidHostController.connect(candidate.address)
                    when(hidResult) {
                        is HidOperationResult.RequiresPrivilege -> _uiEvents.emit(PhoneKeyboardUiEvent.ShowError(PhoneKeyboardFailureReason.PRIVILEGED_HID_HOST_REQUIRED))
                        is HidOperationResult.NotAvailable -> _uiEvents.emit(PhoneKeyboardUiEvent.ShowError(PhoneKeyboardFailureReason.UNSUPPORTED_TS18_STACK))
                        is HidOperationResult.Failed -> _uiEvents.emit(PhoneKeyboardUiEvent.ShowError(PhoneKeyboardFailureReason.HID_SERVICE_SEEN_CONNECT_FAILED))
                        is HidOperationResult.RequiresDeviceValidation -> {
                            // Hid host started, now we wait for input node which will be picked up by the scan controller stream
                        }
                        is HidOperationResult.Started -> {}
                    }
                }
            } catch (e: Exception) {
                _uiEvents.emit(PhoneKeyboardUiEvent.ShowError(PhoneKeyboardFailureReason.UNSUPPORTED_TS18_STACK))
            }
        }
    }

    fun verifyInput(candidate: PhoneKeyboardCandidate) {
        viewModelScope.launch {
            _uiEvents.emit(PhoneKeyboardUiEvent.NavigateToKeyboardTest)
        }
    }
}

sealed interface PhoneKeyboardUiEvent {
    data class ShowError(val reason: PhoneKeyboardFailureReason) : PhoneKeyboardUiEvent
    object NavigateToKeyboardTest : PhoneKeyboardUiEvent
}
