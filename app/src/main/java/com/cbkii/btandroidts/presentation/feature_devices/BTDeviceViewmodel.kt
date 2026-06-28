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
import com.cbkii.btandroidts.domain.peripheral.DiagnosticsExporter
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisorScheduler
import com.cbkii.btandroidts.domain.peripheral.ScanStatus
import com.cbkii.btandroidts.domain.peripheral.TopwayLaneAdapter
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
	private val peripheralSupervisorScheduler: PeripheralSupervisorScheduler,
	private val topwayLaneAdapter: TopwayLaneAdapter,
	private val diagnosticsExporter: DiagnosticsExporter,
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
		val paired = mutableListOf<BluetoothDeviceModel>()
		val available = mutableListOf<BluetoothDeviceModel>()
		val le = mutableListOf<BluetoothLEDeviceModel>()

		inventoryDevices.forEach { device ->
			if (device.bondState == BondStatus.BONDED) {
				paired.add(device.toClassicModel())
			} else if (DeviceTransport.CLASSIC in device.transports) {
				available.add(device.toClassicModel())
			}

			if (DeviceTransport.BLE in device.transports) {
				le.add(device.toLeModel())
			}
		}

		BTDevicesScreenState(
			pairedDevices = paired.toPersistentList(),
			isPairedDevicesLoaded = pairedDevicesLoaded,
			availableDevices = available.toPersistentList(),
			leDevices = le.toPersistentList(),
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
			BTDevicesScreenEvents.OpenTopwayBluetooth -> launchTopwayBluetooth()
			BTDevicesScreenEvents.ShowPeripheralManager -> showMessage("Select a device to manage ACL, HID Host, RFCOMM terminal or BLE GATT actions explicitly.")
			BTDevicesScreenEvents.ShowFileSharing -> showMessage("Use Android Share to send files to BTAndroidTS; outbound transfer delegates to stock Bluetooth OPP.")
			BTDevicesScreenEvents.ManualSupervisorRetry -> manualSupervisorRetry()
			BTDevicesScreenEvents.ExportDiagnostics -> exportDiagnostics()
			BTDevicesScreenEvents.ShowAdvancedTools -> showMessage("Advanced tools remain under device-specific RFCOMM terminal and BLE GATT screens.")
			is BTDevicesScreenEvents.OnBTPermissionChanged -> _hasBtPermission.update { event.isGranted }
			is BTDevicesScreenEvents.OnLocationPermissionChanged -> {
				// nothing to be look for
			}
		}
	}

	private fun launchTopwayBluetooth() = viewModelScope.launch {
		topwayLaneAdapter.launchPhoneBluetoothUi()
			.onFailure { error -> _uiEvents.emit(UiEvents.ShowSnackBar(error.message ?: "Topway Bluetooth UI unavailable")) }
	}

	private fun manualSupervisorRetry() = viewModelScope.launch {
		peripheralSupervisorScheduler.requestManualRetry("dashboard manual retry")
			.onSuccess { _uiEvents.emit(UiEvents.ShowToast("Supervisor reconcile requested")) }
			.onFailure { error -> _uiEvents.emit(UiEvents.ShowSnackBar(error.message ?: "Supervisor reconcile failed")) }
	}

	private fun exportDiagnostics() = viewModelScope.launch {
		diagnosticsExporter.exportLocal()
			.onSuccess { result ->
				_uiEvents.emit(UiEvents.ShowSnackBar("Diagnostics exported: ${result.path}"))
			}
			.onFailure { error ->
				_uiEvents.emit(UiEvents.ShowSnackBar(error.message ?: "Diagnostics export failed"))
			}
	}

	private fun showMessage(message: String) = viewModelScope.launch {
		_uiEvents.emit(UiEvents.ShowToast(message))
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
