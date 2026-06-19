package com.cbkii.btandroidts.presentation.feature_le_connect.state

import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel

data class SelectedCharacteristicState(
	val service: BLEServiceModel? = null,
	val characteristic: BLECharacteristicsModel? = null,
) {
	val isSheetExpanded: Boolean
		get() = characteristic != null
}