package com.cbkii.btandroidts.domain.bluetooth.models

import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothDeviceType
import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode

data class BluetoothDeviceModel(
	val name: String,
	val address: String,
	val mode: BluetoothMode,
	val type: BluetoothDeviceType? = null,
) {
	companion object {
		const val UNNAMED_DEVICE_NAME = "unnamed"
		const val RSSI_UNIT = "dbM"
	}
}
