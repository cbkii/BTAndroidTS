package com.cbkii.btandroidts.presentation.feature_devices.state

import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel
import com.cbkii.btandroidts.domain.peripheral.FeatureCapability
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class BTDevicesScreenState(
	val pairedDevices: ImmutableList<BluetoothDeviceModel> = persistentListOf(),
	val isPairedDevicesLoaded: Boolean = false,
	val availableDevices: ImmutableList<BluetoothDeviceModel> = persistentListOf(),
	val leDevices: ImmutableList<BluetoothLEDeviceModel> = persistentListOf(),
	val inventoryDevices: ImmutableList<UnifiedBluetoothDevice> = persistentListOf(),
	val capabilities: ImmutableList<FeatureCapability> = persistentListOf(),
)
