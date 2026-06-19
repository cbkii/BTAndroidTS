package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.peripheral.ApplicationBluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.data.peripheral.AndroidBluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothProtectionPolicy
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothInventoryMerger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val peripheralModule = module {
	single { BluetoothProtectionPolicy() }
	single { UnifiedBluetoothInventoryMerger(protectionPolicy = get()) }
	singleOf(::AndroidBluetoothBondController) bind BluetoothBondController::class
	singleOf(::ApplicationBluetoothDeviceInventoryRepository) bind BluetoothDeviceInventoryRepository::class
}
