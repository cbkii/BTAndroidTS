package com.cbkii.btandroidts.presentation.navigation.args

import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel

fun BluetoothDeviceModel.toArgs() = BluetoothDeviceArgs(address = address)

fun BluetoothLEDeviceModel.toArgs() =
	BluetoothDeviceArgs(address = deviceModel.address, name = deviceName)