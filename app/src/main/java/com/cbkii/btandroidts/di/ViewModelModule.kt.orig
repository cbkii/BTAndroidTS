package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.presentation.feature_connect.bt_client.BTClientViewModel
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.BluetoothProfileViewModel
import com.cbkii.btandroidts.presentation.feature_connect.bt_server.BTServerViewModel
import com.cbkii.btandroidts.presentation.feature_devices.BTDeviceViewmodel
import com.cbkii.btandroidts.presentation.feature_le_connect.BLEDeviceViewModel
import com.cbkii.btandroidts.presentation.feature_le_server.BLEServerViewModel
import com.cbkii.btandroidts.presentation.feature_settings.AppSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {

	//devices
	viewModelOf(::BTDeviceViewmodel)
	// bl classic
	viewModelOf(::BTClientViewModel)
	viewModelOf(::BTServerViewModel)
	viewModelOf(::BluetoothProfileViewModel)
	//ble
	viewModelOf(::BLEDeviceViewModel)
	viewModelOf(::BLEServerViewModel)
	//settings
	viewModelOf(::AppSettingsViewModel)
}