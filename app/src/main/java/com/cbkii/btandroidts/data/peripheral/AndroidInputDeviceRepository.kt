package com.cbkii.btandroidts.data.peripheral

import android.view.InputDevice
import com.cbkii.btandroidts.domain.peripheral.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AndroidInputDeviceRepository(
	private val policyStore: PeripheralPolicyStore,
) : InputDeviceRepository {
	override fun listInputDevices(): List<AndroidInputDeviceInfo> =
		InputDevice.getDeviceIds()
			.asSequence()
			.mapNotNull { id -> InputDevice.getDevice(id) }
			.map { device ->
				AndroidInputDeviceInfo(
					id = device.id,
					name = device.name.orEmpty(),
					descriptor = device.descriptor.orEmpty(),
					isKeyboard = device.keyboardType != InputDevice.KEYBOARD_TYPE_NONE,
					isPointer = device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE,
					sources = device.sources,
				)
			}
			.toList()

	override fun hasInputDeviceFor(address: BluetoothAddress): Boolean {
		val compact = address.value.replace(":", "")
		return listInputDevices().any { device ->
			device.descriptor.contains(address.value, ignoreCase = true) ||
				device.descriptor.contains(compact, ignoreCase = true) ||
				device.name.contains(address.value, ignoreCase = true)
		}
	}

	override fun getVerificationResult(address: BluetoothAddress): Flow<InputVerificationResult?> =
		policyStore.policy.map { it.inputVerifications[address] }

	override fun getVerificationResults(): Flow<Map<BluetoothAddress, InputVerificationResult>> =
		policyStore.policy.map { it.inputVerifications }

	override suspend fun recordVerification(address: BluetoothAddress, success: Boolean) {
		policyStore.recordInputVerification(address, success, System.currentTimeMillis())
	}
}
