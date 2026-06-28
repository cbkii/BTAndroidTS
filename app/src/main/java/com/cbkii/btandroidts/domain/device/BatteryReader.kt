package com.cbkii.btandroidts.domain.device

import kotlinx.coroutines.flow.Flow

interface BatteryReader {

	val isBatteryCharging: Boolean

	val currentBatteryLevel: Int


	fun batteryLevelFlow(): Flow<Int>
}