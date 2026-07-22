package com.cbkii.btandroidts.presentation.navigation.screens

import com.cbkii.btandroidts.di.viewModelModule
import org.junit.Test
import org.junit.Assert.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import org.koin.dsl.module
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.cbkii.btandroidts.domain.peripheral.AndroidInputDeviceInfo
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.InputVerificationResult
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import com.cbkii.btandroidts.domain.peripheral.BoundedScanState
import com.cbkii.btandroidts.domain.peripheral.FeatureCapability
import com.cbkii.btandroidts.domain.peripheral.BluetoothScanRequest

class FakeInputDeviceRepositoryForKoin : InputDeviceRepository {
    override fun listInputDevices(): List<AndroidInputDeviceInfo> = emptyList()
    override fun hasInputDeviceFor(address: BluetoothAddress): Boolean = false
    override fun getVerificationResult(address: BluetoothAddress): Flow<InputVerificationResult?> = flowOf(null)
    override fun getVerificationResults(): Flow<Map<BluetoothAddress, InputVerificationResult>> = flowOf(emptyMap())
    override suspend fun recordVerification(address: BluetoothAddress, success: Boolean) {}
}

class FakeBluetoothDeviceInventoryRepositoryForKoin : BluetoothDeviceInventoryRepository {
    override val devices = MutableStateFlow<List<UnifiedBluetoothDevice>>(emptyList())
    override val scanState = MutableStateFlow(BoundedScanState())
    override val capabilities = MutableStateFlow<List<FeatureCapability>>(emptyList())
    override val isBluetoothActive: Flow<Boolean> = flowOf(true)
    override val hasBTPermissions: Boolean = true

    override fun refreshBondedDevices(): Result<Unit> = Result.success(Unit)
    override suspend fun startScan(request: BluetoothScanRequest): Result<Unit> = Result.success(Unit)
    override fun stopScan(): Result<Unit> = Result.success(Unit)
    override suspend fun forgetSelected(address: BluetoothAddress): Result<Unit> = Result.success(Unit)
}

class KeyboardTestViewModelKoinTest {

    @Before
    fun setUp() {
        val mockModule = module {
            single<InputDeviceRepository> { FakeInputDeviceRepositoryForKoin() }
            single<BluetoothDeviceInventoryRepository> { FakeBluetoothDeviceInventoryRepositoryForKoin() }
        }
        startKoin {
            modules(viewModelModule, mockModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `verify keyboard test view model can be resolved by koin`() {
        val koin = org.koin.core.context.GlobalContext.get()
        val viewModel = koin.get<KeyboardTestViewModel>()
        assertNotNull("KeyboardTestViewModel should be successfully resolved by Koin", viewModel)
    }
}
