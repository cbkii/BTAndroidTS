package com.cbkii.btandroidts.domain.peripheral

class BluetoothProtectionPolicy(
	private val userProtectedAddresses: Set<BluetoothAddress> = emptySet(),
) {
	fun classify(address: BluetoothAddress, name: String?): DeviceProtection {
		val normalizedName = name.orEmpty().trim()
		return when {
			address in userProtectedAddresses -> DeviceProtection(
				DeviceProtectionStatus.USER_PROTECTED,
				BluetoothLaneOwner.ANDROID_PERIPHERAL,
				"User protected"
			)

			address.value == TOPWAY_CARKIT_ADDRESS || normalizedName.equals(TOPWAY_CARKIT_NAME, ignoreCase = true) ->
				DeviceProtection(
					DeviceProtectionStatus.TS18_AUTOMOTIVE_CONTROLLER,
					BluetoothLaneOwner.TOPWAY_AUTOMOTIVE,
					"TS18 Topway automotive Bluetooth controller"
				)

			normalizedName.contains("carkit", ignoreCase = true) ||
				normalizedName.contains("zlink", ignoreCase = true) ||
				normalizedName.contains("tlink", ignoreCase = true) ->
				DeviceProtection(
					DeviceProtectionStatus.TOPWAY_LANE_OWNED,
					BluetoothLaneOwner.TOPWAY_AUTOMOTIVE,
					"Likely owned by the Topway phone/projection lane"
				)

			else -> DeviceProtection(
				DeviceProtectionStatus.UNPROTECTED,
				BluetoothLaneOwner.ANDROID_PERIPHERAL,
				"Android peripheral lane"
			)
		}
	}

	companion object {
		const val TOPWAY_CARKIT_NAME = "CarKit_blink"
		const val TOPWAY_CARKIT_ADDRESS = "00:87:61:A9:26:26"
	}
}

data class DeviceProtection(
	val status: DeviceProtectionStatus,
	val laneOwner: BluetoothLaneOwner,
	val reason: String,
)
