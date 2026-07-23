package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.di.viewModelModule
import com.cbkii.btandroidts.domain.peripheral.AndroidInputDeviceInfo
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BluetoothScanRequest
import com.cbkii.btandroidts.domain.peripheral.BoundedScanState
import com.cbkii.btandroidts.domain.peripheral.FeatureCapability
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import com.cbkii.btandroidts.domain.peripheral.InputVerificationResult
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardTestViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `application Koin graph resolves KeyboardTestViewModel`() {
        val inputRepository = FakeInputDeviceRepository()
        val inventoryRepository = FakeInventoryRepository()
        val application = koinApplication {
            modules(
                viewModelModule,
                module {
                    single<InputDeviceRepository> { inputRepository }
                    single<BluetoothDeviceInventoryRepository> { inventoryRepository }
                },
            )
        }

        val viewModel = application.koin.get<KeyboardTestViewModel>()

        assertNotNull(viewModel)
        viewModel.viewModelScope.cancel()
        application.close()
    }

    @Test
    fun `initial refresh publishes enumerated input devices`() = runBlocking {
        val keyboard = AndroidInputDeviceInfo(
            id = 7,
            name = "Test keyboard",
            descriptor = "test-keyboard",
            isKeyboard = true,
            isPointer = false,
            sources = 0,
        )
        val inputRepository = FakeInputDeviceRepository(devices = listOf(keyboard))
        val viewModel = KeyboardTestViewModel(inputRepository, FakeInventoryRepository())

        try {
            val state = withTimeout(2_000) {
                viewModel.state.first { it.inputDevices == listOf(keyboard) }
            }

            assertEquals(listOf(keyboard), state.inputDevices)
            assertNull(state.message)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `initial refresh failure exposes retryable error state`() = runBlocking {
        val inputRepository = FakeInputDeviceRepository(
            failure = IllegalStateException("enumeration failed"),
        )
        val viewModel = KeyboardTestViewModel(inputRepository, FakeInventoryRepository())

        try {
            val state = withTimeout(2_000) {
                viewModel.state.first { it.message == KeyboardTestMessage.RefreshFailed }
            }

            assertEquals(KeyboardTestMessage.RefreshFailed, state.message)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `refresh cancellation is not converted into an error message`() {
        val inputRepository = FakeInputDeviceRepository(
            failure = CancellationException("cancelled"),
        )
        val viewModel = KeyboardTestViewModel(inputRepository, FakeInventoryRepository())

        try {
            assertTrue(inputRepository.called.await(2, TimeUnit.SECONDS))
            assertNull(viewModel.state.value.message)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }
}

private class FakeInputDeviceRepository(
    private val devices: List<AndroidInputDeviceInfo> = emptyList(),
    private val failure: Throwable? = null,
) : InputDeviceRepository {
    val called = CountDownLatch(1)

    override fun listInputDevices(): List<AndroidInputDeviceInfo> {
        called.countDown()
        failure?.let { throw it }
        return devices
    }

    override fun hasInputDeviceFor(address: BluetoothAddress): Boolean = false

    override fun getVerificationResult(address: BluetoothAddress): Flow<InputVerificationResult?> =
        flowOf(null)

    override fun getVerificationResults(): Flow<Map<BluetoothAddress, InputVerificationResult>> =
        flowOf(emptyMap())

    override suspend fun recordVerification(address: BluetoothAddress, success: Boolean) = Unit
}

private class FakeInventoryRepository : BluetoothDeviceInventoryRepository {
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
