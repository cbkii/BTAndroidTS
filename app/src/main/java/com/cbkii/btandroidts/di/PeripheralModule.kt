package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.opp.AndroidFileTransferController
import com.cbkii.btandroidts.data.opp.InMemoryOutgoingTransferStore
import com.cbkii.btandroidts.data.peripheral.ApplicationBluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.data.peripheral.AndroidTopwayLaneAdapter
import com.cbkii.btandroidts.data.peripheral.AndroidVendorPackageInspector
import com.cbkii.btandroidts.data.peripheral.AndroidBluetoothBondController
import com.cbkii.btandroidts.data.peripheral.AndroidInputDeviceRepository
import com.cbkii.btandroidts.data.peripheral.AndroidApi29HidHostController
import com.cbkii.btandroidts.data.peripheral.AndroidPeripheralSupervisorScheduler
import com.cbkii.btandroidts.data.datastore.PeripheralPolicyDataStore
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothProtectionPolicy
import com.cbkii.btandroidts.data.peripheral.ApplicationPeripheralSupervisor
import com.cbkii.btandroidts.data.peripheral.DisabledRootBroker
import com.cbkii.btandroidts.data.peripheral.LocalDiagnosticsExporter
import com.cbkii.btandroidts.data.peripheral.LocalTs18DiagnosticsCollector
import com.cbkii.btandroidts.domain.peripheral.DiagnosticsExporter
import com.cbkii.btandroidts.domain.peripheral.FileTransferController
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import com.cbkii.btandroidts.domain.peripheral.OutgoingTransferStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicyStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisorScheduler
import com.cbkii.btandroidts.domain.peripheral.RootBroker
import com.cbkii.btandroidts.domain.peripheral.TopwayLaneAdapter
import com.cbkii.btandroidts.domain.peripheral.TopwayLaneGuard
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsCollector
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothInventoryMerger
import com.cbkii.btandroidts.domain.peripheral.VendorPackageInspector
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val peripheralModule = module {
	single { BluetoothProtectionPolicy() }
	single { UnifiedBluetoothInventoryMerger(protectionPolicy = get()) }
	singleOf(::AndroidVendorPackageInspector) bind VendorPackageInspector::class
	singleOf(::AndroidTopwayLaneAdapter) bind TopwayLaneAdapter::class
	singleOf(::TopwayLaneGuard)
	singleOf(::PeripheralPolicyDataStore) bind PeripheralPolicyStore::class
	singleOf(::AndroidBluetoothBondController) bind BluetoothBondController::class
	singleOf(::ApplicationBluetoothDeviceInventoryRepository) bind BluetoothDeviceInventoryRepository::class
	singleOf(::AndroidApi29HidHostController) bind HidHostController::class
	singleOf(::AndroidInputDeviceRepository) bind InputDeviceRepository::class
	singleOf(::ApplicationPeripheralSupervisor) bind PeripheralSupervisor::class
	singleOf(::AndroidPeripheralSupervisorScheduler) bind PeripheralSupervisorScheduler::class
	singleOf(::DisabledRootBroker) bind RootBroker::class
	singleOf(::LocalTs18DiagnosticsCollector) bind Ts18DiagnosticsCollector::class
	singleOf(::LocalDiagnosticsExporter) bind DiagnosticsExporter::class
	singleOf(::InMemoryOutgoingTransferStore) bind OutgoingTransferStore::class
	singleOf(::AndroidFileTransferController) bind FileTransferController::class
}
