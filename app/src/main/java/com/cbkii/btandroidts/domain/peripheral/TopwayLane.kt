package com.cbkii.btandroidts.domain.peripheral

interface VendorPackageInspector {
	fun inspect(): VendorPackageSnapshot
}

interface TopwayLaneAdapter {
	fun phoneProjectionStatus(): PhoneProjectionStatus
	fun launchPhoneBluetoothUi(): Result<Unit>
}

class TopwayLaneGuard(
	private val inspector: VendorPackageInspector,
) {
	fun canAssignAndroidPeripheralLane(device: UnifiedBluetoothDevice): TopwayLaneDecision {
		if (device.laneOwner == BluetoothLaneOwner.TOPWAY_AUTOMOTIVE) {
			return TopwayLaneDecision.Blocked("Device is already classified as owned by the Topway automotive lane")
		}
		if (device.isProtected) {
			return TopwayLaneDecision.Blocked("Device is protected: ${device.protectionStatus}")
		}
		val snapshot = inspector.inspect()
		val topwayPresent = snapshot.packages.any { it.family == VendorPackageFamily.TOPWAY_BLUETOOTH && it.installed }
		return if (topwayPresent && device.profileStates.keys.any(BluetoothProfileRole::isPhoneProfile)) {
			TopwayLaneDecision.Warn(
				"Topway Bluetooth package is present; phone/audio/contact profiles should stay on the vendor lane"
			)
		} else {
			TopwayLaneDecision.Allow
		}
	}
}

sealed interface TopwayLaneDecision {
	data object Allow : TopwayLaneDecision
	data class Warn(val reason: String) : TopwayLaneDecision
	data class Blocked(val reason: String) : TopwayLaneDecision
}

data class VendorPackageSnapshot(
	val packages: List<VendorPackageStatus>,
)

data class VendorPackageStatus(
	val packageName: String,
	val family: VendorPackageFamily,
	val installed: Boolean,
	val enabled: Boolean,
	val source: EvidenceSource,
)

enum class VendorPackageFamily {
	TOPWAY_BLUETOOTH,
	TOPWAY_SERVICE,
	PROJECTION,
	ANDROID_BLUETOOTH,
	DOFUN,
	UNKNOWN,
}

data class PhoneProjectionStatus(
	val topwayBluetoothPresent: Boolean,
	val topwayBluetoothEnabled: Boolean,
	val androidBluetoothPresent: Boolean,
	val projectionPackages: List<VendorPackageStatus>,
	val evidence: EvidenceSource,
)

enum class EvidenceSource {
	OBSERVED,
	INFERRED,
	HYPOTHESIS,
	REQUIRES_DEVICE_VALIDATION,
	UNSUPPORTED,
}

private fun BluetoothProfileRole.isPhoneProfile(): Boolean =
	this == BluetoothProfileRole.A2DP ||
		this == BluetoothProfileRole.HFP ||
		this == BluetoothProfileRole.AVRCP ||
		this == BluetoothProfileRole.PBAP ||
		this == BluetoothProfileRole.MAP
