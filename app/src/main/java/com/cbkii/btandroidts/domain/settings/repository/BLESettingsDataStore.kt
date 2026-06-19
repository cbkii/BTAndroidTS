package com.cbkii.btandroidts.domain.settings.repository

import com.cbkii.btandroidts.domain.settings.enums.BLEScanPeriodTimmings
import com.cbkii.btandroidts.domain.settings.enums.BLESettingsScanMode
import com.cbkii.btandroidts.domain.settings.enums.BLESettingsSupportedLayer
import com.cbkii.btandroidts.domain.settings.models.BLESettingsModel
import kotlinx.coroutines.flow.Flow

interface BLESettingsDataStore {

	val settingsFlow: Flow<BLESettingsModel>

	suspend fun getSettings(): BLESettingsModel

	suspend fun onUpdateScanPeriod(timming: BLEScanPeriodTimmings)

	suspend fun onIsAdvertiseExtensionChanged(isAdvertiseExtensionsOnly: Boolean)

	suspend fun onUpdateScanMode(scanMode: BLESettingsScanMode)

	suspend fun onUpdateSupportedLayer(layer: BLESettingsSupportedLayer)
}