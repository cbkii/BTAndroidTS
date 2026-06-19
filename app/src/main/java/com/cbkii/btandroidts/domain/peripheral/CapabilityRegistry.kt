package com.cbkii.btandroidts.domain.peripheral

data class FeatureCapability(
	val feature: PeripheralFeature,
	val status: CapabilityStatus,
	val reason: String,
)

enum class PeripheralFeature {
	CLASSIC_SCAN,
	BLE_SCAN,
	BONDING,
	SELECTIVE_UNPAIR,
	HID_HOST,
	HID_INPUT_VERIFICATION,
	OPP_SHARE,
	STOCK_OPP_DELEGATION,
	TOPWAY_TANDEM_STATUS,
	PERSISTENT_SUPERVISION,
	ROOT_BROKER,
	MAGISK_PRIVILEGED_INSTALL,
	USB_DIRECT_EXPORT,
	TS18_DIAGNOSTICS,
}

enum class CapabilityStatus {
	AVAILABLE,
	UNAVAILABLE,
	REQUIRES_PRIVILEGE,
	REQUIRES_ROOT,
	FAILED,
	REQUIRES_DEVICE_VALIDATION,
}

class CapabilityRegistry(
	private val entries: Map<PeripheralFeature, FeatureCapability>,
) {
	fun all(): List<FeatureCapability> =
		PeripheralFeature.entries.map { feature ->
			entries[feature] ?: FeatureCapability(
				feature = feature,
				status = CapabilityStatus.UNAVAILABLE,
				reason = "No capability provider registered"
			)
		}

	fun get(feature: PeripheralFeature): FeatureCapability =
		all().first { it.feature == feature }

	companion object {
		fun ts18Baseline(
			hasBluetooth: Boolean,
			hasBle: Boolean,
			hasPrivilegedPermission: Boolean,
			hasRoot: Boolean,
		): CapabilityRegistry {
			val entries = buildMap {
				put(
					PeripheralFeature.CLASSIC_SCAN,
					FeatureCapability(
						PeripheralFeature.CLASSIC_SCAN,
						if (hasBluetooth) CapabilityStatus.AVAILABLE else CapabilityStatus.UNAVAILABLE,
						if (hasBluetooth) "Android Bluetooth adapter is available" else "No Android Bluetooth adapter"
					)
				)
				put(
					PeripheralFeature.BLE_SCAN,
					FeatureCapability(
						PeripheralFeature.BLE_SCAN,
						if (hasBle) CapabilityStatus.AVAILABLE else CapabilityStatus.UNAVAILABLE,
						if (hasBle) "BLE feature is available" else "Device does not report BLE support"
					)
				)
				put(
					PeripheralFeature.BONDING,
					FeatureCapability(
						PeripheralFeature.BONDING,
						if (hasBluetooth) CapabilityStatus.AVAILABLE else CapabilityStatus.UNAVAILABLE,
						"Bonding must wait for bond-state broadcasts"
					)
				)
				put(
					PeripheralFeature.SELECTIVE_UNPAIR,
					FeatureCapability(
						PeripheralFeature.SELECTIVE_UNPAIR,
						if (hasPrivilegedPermission) CapabilityStatus.AVAILABLE else CapabilityStatus.REQUIRES_PRIVILEGE,
						"Selective unpair is allowed only for a chosen, unprotected device"
					)
				)
				put(
					PeripheralFeature.HID_HOST,
					FeatureCapability(
						PeripheralFeature.HID_HOST,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"HID Host services are declared on TS18; connection and policy still need validation"
					)
				)
				put(
					PeripheralFeature.HID_INPUT_VERIFICATION,
					FeatureCapability(
						PeripheralFeature.HID_INPUT_VERIFICATION,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"Input-device creation must be verified separately from bonding and profile connection"
					)
				)
				put(
					PeripheralFeature.OPP_SHARE,
					FeatureCapability(
						PeripheralFeature.OPP_SHARE,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"OPP components are present; outbound picker/delegation must be tested"
					)
				)
				put(
					PeripheralFeature.STOCK_OPP_DELEGATION,
					FeatureCapability(
						PeripheralFeature.STOCK_OPP_DELEGATION,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"Prefer stock BluetoothOppService until exact TS18 contracts prove insufficient"
					)
				)
				put(
					PeripheralFeature.TOPWAY_TANDEM_STATUS,
					FeatureCapability(
						PeripheralFeature.TOPWAY_TANDEM_STATUS,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"Topway lane is protected and read-only; status display must be evidence-backed"
					)
				)
				put(
					PeripheralFeature.PERSISTENT_SUPERVISION,
					FeatureCapability(
						PeripheralFeature.PERSISTENT_SUPERVISION,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"Reconnects must be explicit, finite, visible, and ACC-tested"
					)
				)
				put(
					PeripheralFeature.ROOT_BROKER,
					FeatureCapability(
						PeripheralFeature.ROOT_BROKER,
						if (hasRoot) CapabilityStatus.REQUIRES_ROOT else CapabilityStatus.UNAVAILABLE,
						"Root broker is limited to fixed allowlisted operations with captured output"
					)
				)
				put(
					PeripheralFeature.MAGISK_PRIVILEGED_INSTALL,
					FeatureCapability(
						PeripheralFeature.MAGISK_PRIVILEGED_INSTALL,
						if (hasRoot) CapabilityStatus.REQUIRES_ROOT else CapabilityStatus.UNAVAILABLE,
						"Privileged install must be systemless and rollback-tested"
					)
				)
				put(
					PeripheralFeature.USB_DIRECT_EXPORT,
					FeatureCapability(
						PeripheralFeature.USB_DIRECT_EXPORT,
						CapabilityStatus.REQUIRES_DEVICE_VALIDATION,
						"Use /storage/usbdiskN fallback when DocumentsUI is absent"
					)
				)
				put(
					PeripheralFeature.TS18_DIAGNOSTICS,
					FeatureCapability(
						PeripheralFeature.TS18_DIAGNOSTICS,
						CapabilityStatus.AVAILABLE,
						"Diagnostics must be user-started, bounded, local-only, and redacted"
					)
				)
			}
			return CapabilityRegistry(entries)
		}
	}
}
