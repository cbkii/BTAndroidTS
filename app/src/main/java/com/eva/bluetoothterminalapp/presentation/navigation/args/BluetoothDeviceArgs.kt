package com.eva.bluetoothterminalapp.presentation.navigation.args

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothDeviceArgs(
	val address: String,
	val name: String? = null,
)
