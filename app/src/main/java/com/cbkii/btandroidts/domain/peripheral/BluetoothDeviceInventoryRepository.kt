package com.cbkii.btandroidts.domain.peripheral

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothDeviceInventoryRepository {
	val devices: StateFlow<List<UnifiedBluetoothDevice>>
	val scanState: StateFlow<BoundedScanState>
	val capabilities: StateFlow<List<FeatureCapability>>
	val isBluetoothActive: Flow<Boolean>
	val hasBTPermissions: Boolean

	fun refreshBondedDevices(): Result<Unit>
	suspend fun startScan(request: BluetoothScanRequest): Result<Unit>
	fun stopScan(): Result<Unit>
	suspend fun forgetSelected(address: BluetoothAddress): Result<Unit>
}
