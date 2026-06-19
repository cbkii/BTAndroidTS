package com.cbkii.btandroidts.domain.peripheral

interface InputDeviceRepository {
	fun listInputDevices(): List<AndroidInputDeviceInfo>
	fun hasInputDeviceFor(address: BluetoothAddress): Boolean
}

data class AndroidInputDeviceInfo(
	val id: Int,
	val name: String,
	val descriptor: String,
	val isKeyboard: Boolean,
	val isPointer: Boolean,
	val sources: Int,
)
