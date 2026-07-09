package com.cbkii.btandroidts.data.peripheral

import com.cbkii.btandroidts.domain.peripheral.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertSame
import org.junit.Assert.assertNotSame
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class FakePeripheralPolicyStore : PeripheralPolicyStore {
    val _policy = MutableStateFlow(PeripheralPolicy())
    override val policy: Flow<PeripheralPolicy> = _policy
    override suspend fun currentPolicy() = _policy.value
    override suspend fun setSupervisionEnabled(enabled: Boolean) {}
    override suspend fun setSafeModeEnabled(enabled: Boolean) {}
    override suspend fun savePeripheral(device: SavedPeripheralRecord) {}
    override suspend fun removeSavedPeripheral(address: BluetoothAddress) {}
    override suspend fun protectDevice(device: ProtectedPeripheralRecord) {}
    override suspend fun removeProtectedDevice(address: BluetoothAddress) {}
    override suspend fun setRetryState(address: BluetoothAddress, retryState: PeripheralRetryState?) {}
    override suspend fun recordResult(address: BluetoothAddress, result: String, atMillis: Long) {}
    override suspend fun applyReconnectResults(results: List<PeripheralReconnectResult>) {}
    override suspend fun recordInputVerification(address: BluetoothAddress, success: Boolean, atMillis: Long) {}
}

class FakeInventoryRepo : BluetoothDeviceInventoryRepository {
    override val devices = MutableStateFlow<List<UnifiedBluetoothDevice>>(emptyList())
    override suspend fun startScan(request: BluetoothScanRequest): Result<Unit> = Result.success(Unit)
    override fun stopScan(): Result<Unit> = Result.success(Unit)
    override fun refreshBondedDevices(): Result<Unit> = Result.success(Unit)
    override suspend fun forgetSelected(address: BluetoothAddress): Result<Unit> = Result.success(Unit)
    override val scanState = MutableStateFlow<BoundedScanState>(BoundedScanState(status = ScanStatus.IDLE))
    override val capabilities = MutableStateFlow<List<FeatureCapability>>(emptyList())
    override val isBluetoothActive = MutableStateFlow(true)
    override val hasBTPermissions = true
}

class FakeHidHost : HidHostController {
    override suspend fun connect(address: BluetoothAddress): HidOperationResult = HidOperationResult.Started
    override suspend fun disconnect(address: BluetoothAddress): HidOperationResult = HidOperationResult.Started
    override suspend fun setConnectionPolicy(address: BluetoothAddress, allowed: Boolean): HidOperationResult = HidOperationResult.Started
    override val profileStates = MutableStateFlow<Map<BluetoothAddress, ProfileConnectionState>>(emptyMap())
}

class ApplicationPeripheralSupervisorMemoizationTest {

    @Test
    fun testMemoizationCacheHits() = runBlocking {
        val policyStore = FakePeripheralPolicyStore()
        val supervisor = ApplicationPeripheralSupervisor(FakeInventoryRepo(), policyStore, FakeHidHost())

        val address1 = BluetoothAddress.requireValid("11:22:33:44:55:66")
        val record1 = SavedPeripheralRecord(address1, "Dev1", ReconnectPolicy(), 100L)

        val address2 = BluetoothAddress.requireValid("AA:BB:CC:DD:EE:FF")
        val retry1 = PeripheralRetryState(attempt = 1, nextAttemptAtMillis = 200L)

        val initialPolicy = PeripheralPolicy(
            savedPeripherals = listOf(record1),
            retryStates = mapOf(address2 to retry1),
            supervisionEnabled = false,
            safeModeEnabled = false
        )

        policyStore._policy.value = initialPolicy

        // Let flow collect
        kotlinx.coroutines.delay(100)

        val state1 = supervisor.state.value
        val list1 = state1.savedPeripherals
        val map1 = state1.activeAttempts

        // 1. Repeated emission with exact same list/map instance
        policyStore._policy.value = initialPolicy
        kotlinx.coroutines.delay(100)

        val state2 = supervisor.state.value
        assertSame("Should return exact same savedPeripherals instance on identical policy", list1, state2.savedPeripherals)
        assertSame("Should return exact same retryStates instance on identical policy", map1, state2.activeAttempts)

        // 2. Repeated emission with new list/map instances but equal contents
        val equalPolicy = initialPolicy.copy(
            savedPeripherals = listOf(record1.copy()),
            retryStates = mapOf(address2 to retry1.copy())
        )
        policyStore._policy.value = equalPolicy
        kotlinx.coroutines.delay(100)

        val state3 = supervisor.state.value
        assertSame("Structural equality (==) should hit cache for savedPeripherals", list1, state3.savedPeripherals)
        assertSame("Structural equality (==) should hit cache for retryStates", map1, state3.activeAttempts)

        // 3. Changed supervisionEnabled / safeModeEnabled only
        val diffFlagPolicy = equalPolicy.copy(
            supervisionEnabled = true,
            safeModeEnabled = true
        )
        policyStore._policy.value = diffFlagPolicy
        kotlinx.coroutines.delay(100)

        val state4 = supervisor.state.value
        assertSame("Cache should hit for savedPeripherals despite flag changes", list1, state4.savedPeripherals)
        assertSame("Cache should hit for retryStates despite flag changes", map1, state4.activeAttempts)

        // 4. Changed saved peripheral contents
        val changedRecord = record1.copy(displayName = "Dev1_Changed")
        val diffSavedPolicy = diffFlagPolicy.copy(
            savedPeripherals = listOf(changedRecord)
        )
        policyStore._policy.value = diffSavedPolicy
        kotlinx.coroutines.delay(100)

        val state5 = supervisor.state.value
        assertNotSame("Cache should miss for changed savedPeripherals", list1, state5.savedPeripherals)
        assertSame("Cache should still hit for retryStates", map1, state5.activeAttempts)

        // 5. Changed retry-state contents
        val changedRetry = retry1.copy(attempt = 2)
        val diffRetryPolicy = diffSavedPolicy.copy(
            retryStates = mapOf(address2 to changedRetry)
        )
        policyStore._policy.value = diffRetryPolicy
        kotlinx.coroutines.delay(100)

        val state6 = supervisor.state.value
        assertNotSame("Cache should miss for changed retryStates", map1, state6.activeAttempts)
    }
}
