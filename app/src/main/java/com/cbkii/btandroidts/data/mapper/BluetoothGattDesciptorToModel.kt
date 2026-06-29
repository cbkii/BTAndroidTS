package com.cbkii.btandroidts.data.mapper

import android.bluetooth.BluetoothGattDescriptor
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPermission
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.util.BLEDescriptorValue
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

fun BluetoothGattDescriptor.toModel(probableName: String? = null): BLEDescriptorModel =
	BLEDescriptorModel(
		uuid = uuid,
		permissions = gattPermissions,
	).apply {
		this.probableName = probableName
	}

// Android API constants for BLE descriptors
private val ENABLE_INDICATION_VALUE = byteArrayOf(0x02, 0x00)
private val ENABLE_NOTIFICATION_VALUE = byteArrayOf(0x01, 0x00)
private val DISABLE_NOTIFICATION_VALUE = byteArrayOf(0x00, 0x00)

val BLEDescriptorModel.descriptorValue: BLEDescriptorValue
	get() = when {
		byteArray.contentEquals(ENABLE_INDICATION_VALUE) -> BLEDescriptorValue.EnableIndication
		byteArray.contentEquals(ENABLE_NOTIFICATION_VALUE) -> BLEDescriptorValue.EnableNotification
		byteArray.contentEquals(DISABLE_NOTIFICATION_VALUE) -> BLEDescriptorValue.DisableNotifyOrIndication
		else -> BLEDescriptorValue.ReadableValue(valueAsString)
	}


private val BluetoothGattDescriptor.gattPermissions: PersistentList<BLEPermission>
	get() = buildList {
		if (permissions and BluetoothGattDescriptor.PERMISSION_READ != 0)
			add(BLEPermission.PERMISSION_READ)
		if (permissions and BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED != 0)
			add(BLEPermission.PERMISSION_READ_ENCRYPTED)
		if (permissions and BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM != 0)
			add(BLEPermission.PERMISSION_READ_ENCRYPTED_MITM)
		if (permissions and BluetoothGattDescriptor.PERMISSION_WRITE != 0)
			add(BLEPermission.PERMISSION_WRITE)
		if (permissions and BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED != 0)
			add(BLEPermission.PERMISSION_WRITE_ENCRYPTED)
		if (permissions and BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED != 0)
			add(BLEPermission.PERMISSION_WRITE_SIGNED)
		if (permissions and BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM != 0)
			add(BLEPermission.PERMISSION_WRITE_SIGNED_MITM)
		if (permissions and BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM != 0)
			add(BLEPermission.PERMISSION_WRITE_ENCRYPTED_MITM)
	}.toPersistentList()