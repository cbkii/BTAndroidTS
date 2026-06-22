package com.cbkii.btandroidts.presentation.feature_settings

import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.settings.models.BLESettingsModel
import com.cbkii.btandroidts.domain.settings.models.BTSettingsModel
import com.cbkii.btandroidts.domain.settings.repository.BLESettingsDataStore
import com.cbkii.btandroidts.domain.settings.repository.BTSettingsDataSore
import com.cbkii.btandroidts.presentation.feature_settings.util.BLESettingsEvent
import com.cbkii.btandroidts.presentation.feature_settings.util.BTSettingsEvent
import com.cbkii.btandroidts.presentation.util.AppViewModel
import com.cbkii.btandroidts.presentation.util.UiEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(
	private val bleDatastore: BLESettingsDataStore,
	private val btDatastore: BTSettingsDataSore,
) : AppViewModel() {

	val bleSettings = bleDatastore.settingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(2000),
			initialValue = BLESettingsModel()
		)

	val btSettings = btDatastore.settingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(2000),
			initialValue = BTSettingsModel()
		)

	val _uiEvents = MutableSharedFlow<UiEvents>()
	override val uiEvents: SharedFlow<UiEvents>
		get() = _uiEvents.asSharedFlow()

	fun onBLEEvents(event: BLESettingsEvent) {
		when (event) {
			is BLESettingsEvent.OnPhyLayerChange -> viewModelScope.launch {
				bleDatastore.onUpdateSupportedLayer(layer = event.layer)
			}

			is BLESettingsEvent.OnScanModeChange -> viewModelScope.launch {
				bleDatastore.onUpdateScanMode(scanMode = event.mode)
			}

			is BLESettingsEvent.OnScanPeriodChange -> viewModelScope.launch {
				bleDatastore.onUpdateScanPeriod(timming = event.timmings)
			}

			is BLESettingsEvent.OnToggleIsLegacyAdvertisement -> viewModelScope.launch {
				bleDatastore.onIsAdvertiseExtensionChanged(event.isLegacy)
			}
		}
	}

	fun onBTClassicEvents(event: BTSettingsEvent) {
		when (event) {
			is BTSettingsEvent.OnCharsetChange -> viewModelScope.launch {
				btDatastore.onCharsetChange(event.charSet)
			}

			is BTSettingsEvent.OnDisplayModeChange -> viewModelScope.launch {
				btDatastore.onDisplayModeChange(event.mode)
			}

			is BTSettingsEvent.OnReceiveNewLineCharChanged -> viewModelScope.launch {
				btDatastore.onNewLineCharChangeForReceive(event.newlineChar)
			}

			is BTSettingsEvent.OnShowTimeStampValueChanged -> viewModelScope.launch {
				btDatastore.onShowTimestampChange(event.isChange)
			}

			is BTSettingsEvent.OnClearInputValueChange -> viewModelScope.launch {
				btDatastore.onClearInputOnSendValueChange(event.canClear)
			}

			is BTSettingsEvent.OnLocalEchoValueChange -> viewModelScope.launch {
				btDatastore.onLocalEchoValueChange(event.isAllowed)
			}

			is BTSettingsEvent.OnSendNewLineCharChanged -> viewModelScope.launch {
				btDatastore.onNewLineCharChangeForSend(event.newlineChar)
			}

			is BTSettingsEvent.OnKeepScreenOnValueChange -> viewModelScope.launch {
				btDatastore.onKeepScreenOnConnectedValueChange(event.isKeepScreenOn)
			}

			is BTSettingsEvent.OnAutoScrollValueChanged -> viewModelScope.launch {
				btDatastore.onAutoScrollValueChange(event.isEnabled)
			}
		}
	}
}