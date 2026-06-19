package com.cbkii.btandroidts.presentation.feature_devices

import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.BluetoothLEScanner
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothScanRequest
import com.cbkii.btandroidts.domain.peripheral.BondStatus
import com.cbkii.btandroidts.domain.peripheral.DeviceTransport
import com.cbkii.btandroidts.domain.peripheral.ScanStatus
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import com.cbkii.btandroidts.presentation.feature_devices.state.BTDevicesScreenEvents
import com.cbkii.btandroidts.presentation.feature_devices.state.BTDevicesScreenState
import com.cbkii.btandroidts.presentation.util.AppViewModel
import com.cbkii.btandroidts.presentation.util.UiEvents
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BTDeviceViewmodel(
	private val inventoryRepository: BluetoothDeviceInventoryRepository,
	private val bLEScanner: BluetoothLEScanner,
) : AppViewModel() {

	private val _isPairedDevicesReady = MutableStateFlow(false)

	val isBTActive = inventoryRepository.isBluetoothActive
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	val isScanning = inventoryRepository.scanState
		.onEach { state ->
			if (state.status == ScanStatus.FAILED && state.lastError != null) {
				_uiEvents.emit(UiEvents.ShowSnackBar(state.lastError))
			}
		}
		.combine(bLEScanner.isScanning) { state, isBleScanning ->
			state.status == ScanStatus.STARTING ||
				state.status == ScanStatus.RUNNING ||
				state.status == ScanStatus.STOPPING ||
				isBleScanning
		}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = false
		)

	val screenState = combine(
		inventoryRepository.devices,
		inventoryRepository.capabilities,
		_isPairedDevicesReady,
	) { inventoryDevices, capabilities, pairedDevicesLoaded ->
		BTDevicesScreenState(
			pairedDevices = inventoryDevices
				.filter { it.bondState == BondStatus.BONDED }
				.map(UnifiedBluetoothDevice::toClassicModel)
				.toPersistentList(),
			isPairedDevicesLoaded = pairedDevicesLoaded,
			availableDevices = inventoryDevices
				.filter { it.bondState != BondStatus.BONDED && DeviceTransport.CLASSIC in it.transports }
				.map(UnifiedBluetoothDevice::toClassicModel)
				.toPersistentList(),
			leDevices = inventoryDevices
				.filter { DeviceTransport.BLE in it.transports }
				.map(UnifiedBluetoothDevice::toLeModel)
				.toPersistentList(),
			inventoryDevices = inventoryDevices.toPersistentList(),
			capabilities = capabilities.toPersistentList(),
		)
	}.onStart {
		// load paired devices
		setPairedDevices()
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(2000),
		initialValue = BTDevicesScreenState()
	)

	private val _hasBtPermission = MutableStateFlow(inventoryRepository.hasBTPermissions)

	private val _uiEvents = MutableSharedFlow<UiEvents>()
	override val uiEvents: SharedFlow<UiEvents>
		get() = _uiEvents.asSharedFlow()


	fun onEvents(event: BTDevicesScreenEvents) {
		when (event) {
			BTDevicesScreenEvents.StartScan -> startInventoryScan(includeClassic = true, includeBle = false)
			BTDevicesScreenEvents.StopScan -> stopInventoryScan()
			BTDevicesScreenEvents.OnStopAnyRunningScan -> stopInventoryScan()
			BTDevicesScreenEvents.StartLEDeviceScan -> startInventoryScan(includeClassic = false, includeBle = true)
			BTDevicesScreenEvents.StopLEDevicesScan -> stopInventoryScan()
			is BTDevicesScreenEvents.OnBTPermissionChanged -> _hasBtPermission.update { event.isGranted }
			is BTDevicesScreenEvents.OnLocationPermissionChanged -> {
				// nothing to be look for
			}
		}
	}

	private fun setPairedDevices() {
		merge(inventoryRepository.isBluetoothActive, _hasBtPermission).onEach { canCheck ->
			if (canCheck != true) return@onEach
			val status = inventoryRepository.refreshBondedDevices()
			status.fold(
				onSuccess = { _isPairedDevicesReady.update { true } },
				onFailure = { exp ->
					val message = exp.message ?: "Some issues in loading paired devices"
					viewModelScope.launch {
						_uiEvents.emit(UiEvents.ShowSnackBar(message))
					}
				},
			)
		}.launchIn(viewModelScope)
	}

	private fun startInventoryScan(includeClassic: Boolean, includeBle: Boolean) = viewModelScope.launch {
		val status = inventoryRepository.startScan(
			BluetoothScanRequest(
				includeClassic = includeClassic,
				includeBle = includeBle,
			)
		)
		status.fold(
			onSuccess = {
				_uiEvents.emit(UiEvents.ShowToast("Bounded scan started"))
			},
			onFailure = { exception ->
				_uiEvents.emit(
					UiEvents.ShowSnackBar(exception.message ?: "SOME ERROR OCCURRED")
				)
			},
		)
	}


	private fun stopInventoryScan() = viewModelScope.launch {
		val status = inventoryRepository.stopScan()
		status.fold(
			onSuccess = {
				_uiEvents.emit(UiEvents.ShowToast("Scan stopped"))
			},
			onFailure = { exception ->
				_uiEvents.emit(
					UiEvents.ShowSnackBar(exception.message ?: "SOME ERROR OCCURRED")
				)
			},
		)
	}

	override fun onCleared() {
		inventoryRepository.stopScan()
		super.onCleared()
	}
}

private fun UnifiedBluetoothDevice.toClassicModel(): BluetoothDeviceModel = BluetoothDeviceModel(
	name = displayName,
	address = address.value,
	mode = mode,
	type = deviceType,
)

private fun UnifiedBluetoothDevice.toLeModel(): BluetoothLEDeviceModel = BluetoothLEDeviceModel(
	deviceModel = BluetoothDeviceModel(
		name = displayName,
		address = address.value,
		mode = when {
			mode == BluetoothMode.BLUETOOTH_DEVICE_UNKNOWN -> BluetoothMode.BLUETOOTH_DEVICE_LE
			else -> mode
		},
		type = deviceType,
	),
	deviceName = displayName,
	rssi = rssi ?: 0,
)
