package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BondStatus
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PeripheralManagerState(
    val pairedDevices: List<UnifiedBluetoothDevice> = emptyList()
)

class PeripheralManagerViewModel(
    inventoryRepository: BluetoothDeviceInventoryRepository
) : ViewModel() {
    val state: StateFlow<PeripheralManagerState> = inventoryRepository.devices
        .map { devices ->
            PeripheralManagerState(
                pairedDevices = devices.filter { it.bondState == BondStatus.BONDED }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PeripheralManagerState())
}