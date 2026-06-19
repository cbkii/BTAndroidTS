package com.cbkii.btandroidts.di

import org.koin.dsl.module

val appModule = module {
	// contains all the modules
	includes(
		bluetoothLEModule,
		bluetoothClassicModule,
		peripheralModule,
		viewModelModule,
		settingsModule,
		deviceModule
	)
}
