package com.cbkii.btandroidts.data.bluetooth_le

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build

/** Narrow, API-gated facade over one Android BluetoothGatt session. */
@SuppressLint("MissingPermission")
internal class AndroidGattTransport(
	val gatt: BluetoothGatt,
) {
	val sessionToken: Any
		get() = gatt

	fun readRemoteRssi(): Boolean = gatt.readRemoteRssi()

	fun discoverServices(): Boolean = gatt.discoverServices()

	fun requestMtu(mtu: Int): Boolean = gatt.requestMtu(mtu)

	fun reconnect(): Boolean = gatt.connect()

	fun disconnect() = gatt.disconnect()

	fun close() = gatt.close()

	fun readCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
		return gatt.readCharacteristic(characteristic)
	}

	fun writeCharacteristic(
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray,
	): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			gatt.writeCharacteristic(characteristic, value, characteristic.writeType) ==
					BluetoothStatusCodes.SUCCESS
		} else {
			@Suppress("DEPRECATION")
			characteristic.value = value
			@Suppress("DEPRECATION")
			gatt.writeCharacteristic(characteristic)
		}
	}

	fun readDescriptor(descriptor: BluetoothGattDescriptor): Boolean {
		return gatt.readDescriptor(descriptor)
	}

	fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
		} else {
			@Suppress("DEPRECATION")
			descriptor.value = value
			@Suppress("DEPRECATION")
			gatt.writeDescriptor(descriptor)
		}
	}

	fun setCharacteristicNotification(
		characteristic: BluetoothGattCharacteristic,
		enable: Boolean,
	): Boolean = gatt.setCharacteristicNotification(characteristic, enable)
}
