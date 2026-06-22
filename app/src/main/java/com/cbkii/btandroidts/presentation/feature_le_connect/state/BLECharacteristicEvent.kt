package com.cbkii.btandroidts.presentation.feature_le_connect.state

import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel

sealed interface BLECharacteristicEvent {

	data class OnSelectCharacteristic(
		val service: BLEServiceModel,
		val characteristics: BLECharacteristicsModel
	) : BLECharacteristicEvent

	data class OnDescriptorRead(val desc: BLEDescriptorModel) : BLECharacteristicEvent

	data object OnUnSelectCharacteristic : BLECharacteristicEvent

	data object ReadCharacteristic : BLECharacteristicEvent

	data object WriteCharacteristic : BLECharacteristicEvent

	data object OnIndicateCharacteristic : BLECharacteristicEvent

	data object OnNotifyCharacteristic : BLECharacteristicEvent

	data object OnStopNotifyOrIndication : BLECharacteristicEvent
}