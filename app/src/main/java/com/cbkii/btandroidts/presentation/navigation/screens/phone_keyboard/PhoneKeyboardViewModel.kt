package com.cbkii.btandroidts.presentation.navigation.screens.phone_keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardCandidate
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardScanController
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhoneKeyboardViewModel(
    private val scanController: PhoneKeyboardScanController,
    private val bondController: BluetoothBondController,
    private val hidHostController: HidHostController,
    private val inputDeviceRepository: InputDeviceRepository
) : ViewModel() {

    val candidates = scanController.candidates
    val isScanning = scanController.isScanning

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
            if (!candidate.isBonded) {
                bondController.createBond(candidate.address)
            }
            hidHostController.connect(candidate.address)
        }
    }

    fun verifyInput(candidate: PhoneKeyboardCandidate, navigator: com.ramcosta.composedestinations.navigation.DestinationsNavigator) {
        // Navigate to the existing KeyboardTestScreen so the user can test the input
        navigator.navigate(com.ramcosta.composedestinations.generated.destinations.KeyboardTestDestination)
    }
}
