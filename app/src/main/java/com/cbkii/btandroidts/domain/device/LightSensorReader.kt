package com.cbkii.btandroidts.domain.device

import kotlinx.coroutines.flow.Flow

interface LightSensorReader {

	suspend fun readCurrentValue(): Float?

	fun readValuesFlow(): Flow<Float>
}