package com.cbkii.btandroidts.presentation.feature_connect.bt_profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.bluetooth.BluetoothClientConnector
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.state.BTProfileEvents
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.state.BTProfileScreenState
import com.cbkii.btandroidts.presentation.navigation.args.BluetoothDeviceArgs
import com.cbkii.btandroidts.presentation.util.AppViewModel
import com.cbkii.btandroidts.presentation.util.UiEvents
import com.ramcosta.composedestinations.generated.navArgs
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cbkii.btandroidts.domain.bluetooth.ConnectionOrchestrator
import java.util.UUID

sealed interface ProfileNavigationEvent {
	data class NavigateToRFCOMM(val uuid: UUID) : ProfileNavigationEvent
	object NavigateToHID : ProfileNavigationEvent
	object NavigateToOPP : ProfileNavigationEvent
	object NavigateToBLE : ProfileNavigationEvent
	object Stay : ProfileNavigationEvent
}

class BluetoothProfileViewModel(
	private val clientConnector: BluetoothClientConnector,
	private val savedStateHandle: SavedStateHandle,
) : AppViewModel() {

	private val _profile = MutableStateFlow(BTProfileScreenState())
	val profile = _profile
		.onStart { loadDeviceUUIDs() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000L),
			initialValue = BTProfileScreenState()
		)

	private val _uiEvents = MutableSharedFlow<UiEvents>()
	override val uiEvents: SharedFlow<UiEvents>
		get() = _uiEvents.asSharedFlow()

	private val _navEvents = MutableSharedFlow<ProfileNavigationEvent>(extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
	val navEvents = _navEvents.asSharedFlow()

	private val device: BluetoothDeviceArgs?
		get() = savedStateHandle.navArgs()


	fun onEvent(event: BTProfileEvents) {
		when (event) {
			BTProfileEvents.OnRetryFetchUUID -> refreshUUIDs()

		}
	}

	fun onConnectSelected(uuid: UUID) = viewModelScope.launch {
		val strategy = ConnectionOrchestrator.determineStrategy(uuid)
		val event = when (strategy) {
			com.cbkii.btandroidts.domain.bluetooth.ConnectionRouteStrategy.HID_HOST -> ProfileNavigationEvent.NavigateToHID
			com.cbkii.btandroidts.domain.bluetooth.ConnectionRouteStrategy.OPP_FILE_TRANSFER -> ProfileNavigationEvent.NavigateToOPP
			com.cbkii.btandroidts.domain.bluetooth.ConnectionRouteStrategy.RFCOMM_TERMINAL -> ProfileNavigationEvent.NavigateToRFCOMM(uuid)
			else -> {
				_uiEvents.emit(UiEvents.ShowSnackBar("Unsupported profile routing for UUID $uuid"))
				return@launch
			}
		}
		_navEvents.emit(event)
	}

	fun onTryAllMethods() = viewModelScope.launch {
		val bestUuid = ConnectionOrchestrator.getBestMethod(_profile.value.deviceUUIDS)
		bestUuid?.let { onConnectSelected(it) }
			?: _uiEvents.emit(UiEvents.ShowSnackBar("No supported automatic connection methods found"))
	}

	private fun loadDeviceUUIDs() = viewModelScope.launch {
		val address = device?.address ?: return@launch
		val result = clientConnector.loadDeviceFeatureUUID(address)
		result.fold(
			onSuccess = { ids ->
				_profile.update {
					it.copy(
						deviceUUIDS = ids.toPersistentList(),
						isDiscovering = false
					)
				}
			},
			onFailure = {
				_uiEvents.emit(UiEvents.ShowSnackBar("Unable to load features refresh to continue"))
			},
		)
	}

	private fun refreshUUIDs() {

		val isDiscovering = _profile.value.isDiscovering
		if (isDiscovering) return
		val address = device?.address ?: return

		_profile.update { it.copy(isDiscovering = true) }
		clientConnector.refreshDeviceFeatureUUID(address = address)
			.catch { err -> err.printStackTrace() }
			.onEach { deviceUUIDs ->
				_profile.update { state ->
					state.copy(
						deviceUUIDS = deviceUUIDs.toImmutableList(),
						isDiscovering = false
					)
				}
			}
			.launchIn(viewModelScope)
	}

}