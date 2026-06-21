package com.cbkii.btandroidts.data.peripheral

import android.content.Context
import android.content.pm.PackageManager
import com.cbkii.btandroidts.domain.peripheral.EvidenceSource
import com.cbkii.btandroidts.domain.peripheral.VendorPackageFamily
import com.cbkii.btandroidts.domain.peripheral.VendorPackageInspector
import com.cbkii.btandroidts.domain.peripheral.VendorPackageSnapshot
import com.cbkii.btandroidts.domain.peripheral.VendorPackageStatus

class AndroidVendorPackageInspector(
	context: Context,
) : VendorPackageInspector {

	private val appContext = context.applicationContext
	private val packageManager = appContext.packageManager

	override fun inspect(): VendorPackageSnapshot =
		VendorPackageSnapshot(
			packages = KNOWN_PACKAGES.map { candidate ->
				val installed = isInstalled(candidate.packageName)
				VendorPackageStatus(
					packageName = candidate.packageName,
					family = candidate.family,
					installed = installed,
					enabled = installed && isEnabled(candidate.packageName),
					source = EvidenceSource.INFERRED,
				)
			}
		)

	private fun isInstalled(packageName: String): Boolean =
		runCatching {
			@Suppress("DEPRECATION")
			packageManager.getPackageInfo(packageName, 0)
		}.isSuccess

	private fun isEnabled(packageName: String): Boolean =
		runCatching {
			val state = packageManager.getApplicationEnabledSetting(packageName)
			state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
				state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
		}.getOrDefault(false)

	private data class PackageCandidate(
		val packageName: String,
		val family: VendorPackageFamily,
	)

	companion object {
		private val KNOWN_PACKAGES = listOf(
			PackageCandidate("com.android.bluetooth", VendorPackageFamily.ANDROID_BLUETOOTH),
			PackageCandidate("com.tw.bt", VendorPackageFamily.TOPWAY_BLUETOOTH),
			PackageCandidate("com.tw.service", VendorPackageFamily.TOPWAY_SERVICE),
			PackageCandidate("com.tw.core", VendorPackageFamily.TOPWAY_SERVICE),
			PackageCandidate("com.tw.coreservice", VendorPackageFamily.TOPWAY_SERVICE),
			PackageCandidate("com.tw.carinfoservice", VendorPackageFamily.TOPWAY_SERVICE),
			PackageCandidate("com.tw.music", VendorPackageFamily.DOFUN),
			PackageCandidate("com.tw.media", VendorPackageFamily.DOFUN),
			PackageCandidate("com.zlink", VendorPackageFamily.PROJECTION),
			PackageCandidate("com.zjinnova.zlink", VendorPackageFamily.PROJECTION),
			PackageCandidate("com.tlink", VendorPackageFamily.PROJECTION),
			PackageCandidate("com.syu.ms", VendorPackageFamily.PROJECTION),
		)
	}
}
