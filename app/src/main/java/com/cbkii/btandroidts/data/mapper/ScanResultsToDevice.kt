package com.cbkii.btandroidts.data.mapper

import android.bluetooth.le.ScanResult
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel

fun ScanResult.toDomainModel(): BluetoothLEDeviceModel = BluetoothLEDeviceModel(
	deviceModel = device.toDomainModel(),
	deviceName = scanRecord?.deviceName ?: BluetoothDeviceModel.UNNAMED_DEVICE_NAME,
	rssi = rssi,
)