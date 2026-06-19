package com.cbkii.btandroidts.data.peripheral

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.cbkii.btandroidts.domain.bluetooth.BluetoothScanner
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.BluetoothLEScanner
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothScanRequest
import com.cbkii.btandroidts.domain.peripheral.BondStatus
import com.cbkii.btandroidts.domain.peripheral.BoundedScanState
import com.cbkii.btandroidts.domain.peripheral.CapabilityRegistry
import com.cbkii.btandroidts.domain.peripheral.DeviceProtectionStatus
import com.cbkii.btandroidts.domain.peripheral.DeviceTransport
import com.cbkii.btandroidts.domain.peripheral.FeatureCapability
import com.cbkii.btandroidts.domain.peripheral.ScanStatus
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothInventoryMerger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApplicationBluetoothDeviceInventoryRepository(
	context: Context,
	private val bluetoothScanner: BluetoothScanner,
	private val bleScanner: BluetoothLEScanner,
	private val merger: UnifiedBluetoothInventoryMerger,
	private val bondController: BluetoothBondController,
) : BluetoothDeviceInventoryRepository {

	private val appContext = context.applicationContext
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private val _devices = MutableStateFlow<List<UnifiedBluetoothDevice>>(emptyList())
	override val devices: StateFlow<List<UnifiedBluetoothDevice>> = _devices.asStateFlow()

	private val _scanState = MutableStateFlow(BoundedScanState())
	override val scanState: StateFlow<BoundedScanState> = _scanState.asStateFlow()

	override val capabilities: StateFlow<List<FeatureCapability>> =
		MutableStateFlow(
			CapabilityRegistry.ts18Baseline(
				hasBluetooth = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
				hasBle = bleScanner.hasBTLEFeature,
				hasPrivilegedPermission = hasPermission(Manifest.permission.BLUETOOTH_PRIVILEGED),
				hasRoot = false,
			).all()
		).asStateFlow()

	override val isBluetoothActive: Flow<Boolean> =
		bluetoothScanner.isBluetoothActive.distinctUntilChanged()

	override val hasBTPermissions: Boolean
		get() = bluetoothScanner.hasBTPermissions

	private var scanJob: Job? = null
	private var bleScanJob: Job? = null
	private var bondedReceiverRequested = false

	init {
		scope.launch {
			combine(
				bluetoothScanner.pairedDevices,
				bluetoothScanner.availableDevices,
				bleScanner.leDevices,
			) { paired, classicAvailable, bleAvailable ->
				InventorySourceSnapshot(
					pairedDevices = paired,
					classicAvailable = classicAvailable,
					bleAvailable = bleAvailable,
					nowMillis = nowMillis(),
				)
			}.collect { snapshot ->
				_devices.update { current ->
					val withPaired = merger.mergeClassicBonded(current, snapshot.pairedDevices, snapshot.nowMillis)
					val withClassic = snapshot.classicAvailable.fold(withPaired) { inventory, device ->
						merger.mergeClassicDiscovered(inventory, device, rssi = null, nowMillis = snapshot.nowMillis)
					}
					val withBle = snapshot.bleAvailable.fold(withClassic) { inventory, device ->
						merger.mergeBleDiscovered(inventory, device, snapshot.nowMillis)
					}
					merger.expireStaleDiscoveries(withBle, snapshot.nowMillis)
				}
			}
		}
	}

	override fun refreshBondedDevices(): Result<Unit> {
		if (bondedReceiverRequested) return Result.success(Unit)
		return bluetoothScanner.findPairedDevices()
			.onSuccess { bondedReceiverRequested = true }
	}

	override suspend fun startScan(request: BluetoothScanRequest): Result<Unit> {
		stopScan()
		val startedAt = nowMillis()
		val transports = buildSet {
			if (request.includeClassic) add(DeviceTransport.CLASSIC)
			if (request.includeBle) add(DeviceTransport.BLE)
		}
		_scanState.value = BoundedScanState(
			status = ScanStatus.STARTING,
			startedAtMillis = startedAt,
			endsAtMillis = startedAt + request.durationMillis,
			activeTransports = transports,
		)

		scanJob = scope.launch {
			try {
				startRequestedScans(request)
				_scanState.update { it.copy(status = ScanStatus.RUNNING, lastError = null) }
				delay(request.durationMillis)
			} catch (cancellation: CancellationException) {
				throw cancellation
			} catch (error: Throwable) {
				_scanState.update {
					it.copy(status = ScanStatus.FAILED, lastError = error.message ?: error::class.simpleName)
				}
			} finally {
				stopUnderlyingScans()
				_scanState.update {
					if (it.status == ScanStatus.FAILED) it.copy(activeTransports = emptySet())
					else BoundedScanState()
				}
			}
		}
		return Result.success(Unit)
	}

	override fun stopScan(): Result<Unit> {
		scanJob?.cancel()
		scanJob = null
		bleScanJob?.cancel()
		bleScanJob = null
		_scanState.update {
			if (it.status == ScanStatus.IDLE) it else it.copy(status = ScanStatus.STOPPING)
		}
		stopUnderlyingScans()
		_scanState.value = BoundedScanState()
		return Result.success(Unit)
	}

	override suspend fun forgetSelected(address: BluetoothAddress): Result<Unit> {
		val device = devices.value.firstOrNull { it.address == address }
			?: return Result.failure(IllegalArgumentException("Device $address is not in inventory"))

		return when {
			device.protectionStatus != DeviceProtectionStatus.UNPROTECTED ->
				Result.failure(IllegalStateException("Protected device cannot be forgotten: ${device.protectionStatus}"))
			device.bondState != BondStatus.BONDED ->
				Result.failure(IllegalStateException("Only bonded devices can be forgotten"))
			else -> when (val result = bondController.removeBond(address)) {
				BondingResult.Removed -> Result.success(Unit)
				is BondingResult.Failed -> Result.failure(
					IllegalStateException("Failed to forget ${address.value}: ${result.reason} ${result.detail.orEmpty()}")
				)
				else -> Result.failure(IllegalStateException("Unexpected forget result: $result"))
			}
		}
	}

	private fun startRequestedScans(request: BluetoothScanRequest) {
		if (request.includeClassic) {
			bluetoothScanner.startScan().getOrThrow()
		}
		if (request.includeBle) {
			bleScanJob?.cancel()
			bleScanJob = scope.launch { bleScanner.startDiscovery() }
		}
	}

	private fun stopUnderlyingScans() {
		bleScanJob?.cancel()
		bleScanJob = null
		bluetoothScanner.stopScan()
		bleScanner.stopDiscovery()
	}

	private fun hasPermission(permission: String): Boolean =
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) true
		else ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

	private fun nowMillis(): Long = System.currentTimeMillis()

	private data class InventorySourceSnapshot(
		val pairedDevices: List<BluetoothDeviceModel> = emptyList(),
		val classicAvailable: List<BluetoothDeviceModel> = emptyList(),
		val bleAvailable: List<BluetoothLEDeviceModel> = emptyList(),
		val nowMillis: Long = 0L,
	)
}
