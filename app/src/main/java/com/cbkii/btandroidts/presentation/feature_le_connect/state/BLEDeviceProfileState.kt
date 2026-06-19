package com.cbkii.btandroidts.presentation.feature_le_connect.state

import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEConnectionState
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class BLEDeviceProfileState(
	val connectionState: BLEConnectionState = BLEConnectionState.FAILED,
	val device: BluetoothDeviceModel? = null,
	val signalStrength: Int = 0,
	val services: ImmutableList<BLEServiceModel> = persistentListOf(),
)
