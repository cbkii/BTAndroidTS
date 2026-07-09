package com.cbkii.btandroidts.domain.peripheral

import kotlinx.coroutines.flow.Flow

interface InputDeviceRepository {
	fun listInputDevices(): List<AndroidInputDeviceInfo>
	fun hasInputDeviceFor(address: BluetoothAddress): Boolean
	fun getVerificationResult(address: BluetoothAddress): Flow<InputVerificationResult?>
	fun getVerificationResults(): Flow<Map<BluetoothAddress, InputVerificationResult>>
	suspend fun recordVerification(address: BluetoothAddress, success: Boolean)
}

data class InputVerificationResult(
	val success: Boolean,
	val timestampMillis: Long,
)

data class AndroidInputDeviceInfo(
	val id: Int,
	val name: String,
	val descriptor: String,
	val isKeyboard: Boolean,
	val isPointer: Boolean,
	val sources: Int,
)
