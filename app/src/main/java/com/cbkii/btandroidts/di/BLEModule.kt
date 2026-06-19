package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.ble_server.AndroidBLEServerConnector
import com.cbkii.btandroidts.data.bluetooth_le.AndroidBLEClientConnector
import com.cbkii.btandroidts.data.bluetooth_le.AndroidBluetoothLEScanner
import com.cbkii.btandroidts.data.samples.SampleUUIDReader
import com.cbkii.btandroidts.domain.bluetooth_le.BLEServerConnector
import com.cbkii.btandroidts.domain.bluetooth_le.BluetoothLEClientConnector
import com.cbkii.btandroidts.domain.bluetooth_le.BluetoothLEScanner
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val bluetoothLEModule = module {

	singleOf(::SampleUUIDReader)

	factoryOf(::AndroidBluetoothLEScanner).bind<BluetoothLEScanner>()
	factoryOf(::AndroidBLEClientConnector).bind<BluetoothLEClientConnector>()
	factoryOf(::AndroidBLEServerConnector).bind<BLEServerConnector>()
}