package com.cbkii.btandroidts.data.peripheral

import android.view.InputDevice
import com.cbkii.btandroidts.domain.peripheral.AndroidInputDeviceInfo
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository

class AndroidInputDeviceRepository : InputDeviceRepository {
	override fun listInputDevices(): List<AndroidInputDeviceInfo> =
		InputDevice.getDeviceIds()
			.mapNotNull(InputDevice::getDevice)
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

	override fun hasInputDeviceFor(address: BluetoothAddress): Boolean {
		val compact = address.value.replace(":", "")
		return listInputDevices().any { device ->
			device.descriptor.contains(address.value, ignoreCase = true) ||
				device.descriptor.contains(compact, ignoreCase = true) ||
				device.name.contains(address.value, ignoreCase = true)
		}
	}
}
