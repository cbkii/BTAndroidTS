package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.bluetooth.AndroidBTClientConnector
import com.cbkii.btandroidts.data.bluetooth.AndroidBTServerConnector
import com.cbkii.btandroidts.data.bluetooth.AndroidBluetoothScanner
import com.cbkii.btandroidts.domain.bluetooth.BluetoothClientConnector
import com.cbkii.btandroidts.domain.bluetooth.BluetoothScanner
import com.cbkii.btandroidts.domain.bluetooth.BluetoothServerConnector
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val bluetoothClassicModule = module {

	factoryOf(::AndroidBluetoothScanner) bind BluetoothScanner::class

	factoryOf(::AndroidBTClientConnector) bind BluetoothClientConnector::class

	factoryOf(::AndroidBTServerConnector) bind BluetoothServerConnector::class

}