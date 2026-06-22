package com.cbkii.btandroidts.presentation.navigation.args

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class BluetoothClientConnectArgs(
	val address: String,
	val uuid: Uuid,
)
