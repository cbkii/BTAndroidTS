package com.cbkii.btandroidts.data.peripheral

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothReconcileReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		when (intent.action) {
			Intent.ACTION_BOOT_COMPLETED -> PeripheralReconcileJobService.schedule(
				context = context,
				reason = "boot completed"
			)
			Intent.ACTION_MY_PACKAGE_REPLACED -> PeripheralReconcileJobService.schedule(
				context = context,
				reason = "package replaced"
			)
			BluetoothAdapter.ACTION_STATE_CHANGED -> {
				val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
				if (state == BluetoothAdapter.STATE_ON) {
					PeripheralReconcileJobService.schedule(
						context = context,
						reason = "Bluetooth adapter on"
					)
				}
			}
		}
	}
}
