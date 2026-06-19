package com.cbkii.btandroidts.domain.peripheral

import kotlinx.coroutines.flow.Flow

interface HidHostController {
	val profileStates: Flow<Map<BluetoothAddress, ProfileConnectionState>>

	suspend fun connect(address: BluetoothAddress): HidOperationResult
	suspend fun disconnect(address: BluetoothAddress): HidOperationResult
	suspend fun setConnectionPolicy(address: BluetoothAddress, allowed: Boolean): HidOperationResult
}

sealed interface HidOperationResult {
	data object Started : HidOperationResult
	data object NotAvailable : HidOperationResult
	data object RequiresPrivilege : HidOperationResult
	data object RequiresDeviceValidation : HidOperationResult
	data class Failed(val reason: String) : HidOperationResult
}
