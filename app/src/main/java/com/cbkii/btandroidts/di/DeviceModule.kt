package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.device.BatteryReaderImpl
import com.cbkii.btandroidts.data.device.LightSensorReaderImpl
import com.cbkii.btandroidts.domain.device.BatteryReader
import com.cbkii.btandroidts.domain.device.LightSensorReader
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val deviceModule = module {
	singleOf(::BatteryReaderImpl).bind<BatteryReader>()
	singleOf(::LightSensorReaderImpl).bind<LightSensorReader>()
}