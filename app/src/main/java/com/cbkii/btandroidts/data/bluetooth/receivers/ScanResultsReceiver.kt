package com.cbkii.btandroidts.data.bluetooth.receivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cbkii.btandroidts.data.mapper.toDomainModel
import com.cbkii.btandroidts.data.utils.hasBTScanPermission
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel

@Suppress("DEPRECATION")
class ScanResultsReceiver(
	private val onDevice: (BluetoothDeviceModel) -> Unit
) : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		// check for intent and action
		if (intent == null || context?.hasBTScanPermission != true) return
		// matches the correct action
		if (intent.action != BluetoothDevice.ACTION_FOUND) return

		val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
		else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

		device?.toDomainModel()?.let(onDevice)
	}
}