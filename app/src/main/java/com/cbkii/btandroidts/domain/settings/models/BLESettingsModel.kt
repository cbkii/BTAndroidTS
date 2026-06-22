package com.cbkii.btandroidts.domain.settings.models

import com.cbkii.btandroidts.domain.settings.enums.BLEScanPeriodTimmings
import com.cbkii.btandroidts.domain.settings.enums.BLESettingsScanMode
import com.cbkii.btandroidts.domain.settings.enums.BLESettingsSupportedLayer

data class BLESettingsModel(
	val scanPeriod: BLEScanPeriodTimmings = BLEScanPeriodTimmings.FIVE_MINUTES,
	val supportedLayer: BLESettingsSupportedLayer = BLESettingsSupportedLayer.ALL,
	val scanMode: BLESettingsScanMode = BLESettingsScanMode.BALANCED,
	val isLegacyOnly: Boolean = true
)
