package com.cbkii.btandroidts.domain.bluetooth.models

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class BluetoothMessage(
	val message: String,
	val type: BluetoothMessageType,
	val uuid: Uuid = Uuid.random(),
	val logTime: Instant = Clock.System.now(),
)