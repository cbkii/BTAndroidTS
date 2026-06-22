package com.cbkii.btandroidts.data.peripheral

import android.content.Context
import android.content.Intent
import com.cbkii.btandroidts.domain.peripheral.EvidenceSource
import com.cbkii.btandroidts.domain.peripheral.PhoneProjectionStatus
import com.cbkii.btandroidts.domain.peripheral.TopwayLaneAdapter
import com.cbkii.btandroidts.domain.peripheral.VendorPackageFamily
import com.cbkii.btandroidts.domain.peripheral.VendorPackageInspector

class AndroidTopwayLaneAdapter(
	context: Context,
	private val inspector: VendorPackageInspector,
) : TopwayLaneAdapter {

	private val appContext = context.applicationContext
	private val packageManager = appContext.packageManager

	override fun phoneProjectionStatus(): PhoneProjectionStatus {
		val packages = inspector.inspect().packages
		val topwayBluetooth = packages.firstOrNull { it.packageName == TOPWAY_BLUETOOTH_PACKAGE }
		val androidBluetooth = packages.firstOrNull { it.packageName == ANDROID_BLUETOOTH_PACKAGE }
		return PhoneProjectionStatus(
			topwayBluetoothPresent = topwayBluetooth?.installed == true,
			topwayBluetoothEnabled = topwayBluetooth?.enabled == true,
			androidBluetoothPresent = androidBluetooth?.installed == true,
			projectionPackages = packages.filter { it.family == VendorPackageFamily.PROJECTION && it.installed },
			evidence = EvidenceSource.INFERRED,
		)
	}

	override fun launchPhoneBluetoothUi(): Result<Unit> {
		val launchIntent = packageManager.getLaunchIntentForPackage(TOPWAY_BLUETOOTH_PACKAGE)
			?: return Result.failure(IllegalStateException("Topway Bluetooth UI package is not launchable"))
		launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		return runCatching { appContext.startActivity(launchIntent) }
	}

	private companion object {
		const val TOPWAY_BLUETOOTH_PACKAGE = "com.tw.bt"
		const val ANDROID_BLUETOOTH_PACKAGE = "com.android.bluetooth"
	}
}
