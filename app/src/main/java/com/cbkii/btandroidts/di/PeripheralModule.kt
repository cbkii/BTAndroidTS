package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.peripheral.ApplicationBluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.data.peripheral.AndroidBluetoothBondController
import com.cbkii.btandroidts.data.peripheral.AndroidInputDeviceRepository
import com.cbkii.btandroidts.data.peripheral.AndroidApi29HidHostController
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothProtectionPolicy
import com.cbkii.btandroidts.data.peripheral.ApplicationPeripheralSupervisor
import com.cbkii.btandroidts.data.peripheral.DisabledRootBroker
import com.cbkii.btandroidts.data.peripheral.LocalTs18DiagnosticsCollector
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import com.cbkii.btandroidts.domain.peripheral.RootBroker
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsCollector
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothInventoryMerger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val peripheralModule = module {
	single { BluetoothProtectionPolicy() }
	single { UnifiedBluetoothInventoryMerger(protectionPolicy = get()) }
	singleOf(::AndroidBluetoothBondController) bind BluetoothBondController::class
	singleOf(::ApplicationBluetoothDeviceInventoryRepository) bind BluetoothDeviceInventoryRepository::class
	singleOf(::AndroidApi29HidHostController) bind HidHostController::class
	singleOf(::AndroidInputDeviceRepository) bind InputDeviceRepository::class
	singleOf(::ApplicationPeripheralSupervisor) bind PeripheralSupervisor::class
	singleOf(::DisabledRootBroker) bind RootBroker::class
	singleOf(::LocalTs18DiagnosticsCollector) bind Ts18DiagnosticsCollector::class
}
