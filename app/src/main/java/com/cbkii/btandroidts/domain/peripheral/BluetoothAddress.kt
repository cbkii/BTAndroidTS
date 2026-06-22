package com.cbkii.btandroidts.domain.peripheral

private val MAC_ADDRESS_PATTERN = Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$")

@JvmInline
value class BluetoothAddress private constructor(val value: String) {

	override fun toString(): String = value

	companion object {
		fun parse(address: String): BluetoothAddress? {
			val normalized = address.trim().uppercase()
			if (!MAC_ADDRESS_PATTERN.matches(normalized)) return null
			return BluetoothAddress(normalized)
		}

		fun requireValid(address: String): BluetoothAddress =
			parse(address) ?: error("Invalid Bluetooth MAC address: $address")
	}
}
