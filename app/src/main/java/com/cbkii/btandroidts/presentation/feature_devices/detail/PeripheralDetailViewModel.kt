package com.cbkii.btandroidts.presentation.feature_devices.detail

import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.*
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import com.cbkii.btandroidts.presentation.util.AppViewModel
import com.cbkii.btandroidts.presentation.util.UiEvents
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PeripheralDetailState(
    val device: UnifiedBluetoothDevice? = null,
    val savedRecord: SavedPeripheralRecord? = null,
    val protectedRecord: ProtectedPeripheralRecord? = null,
    val retryState: PeripheralRetryState? = null,
    val isBusy: Boolean = false,
    val isAddressValid: Boolean = true
)

data class PeripheralDetailRequest(
    val address: BluetoothAddress?,
    val fallbackName: String?,
) {
    companion object {
        fun fromRaw(address: String, name: String): PeripheralDetailRequest =
            PeripheralDetailRequest(
                address = BluetoothAddress.parse(address),
                fallbackName = name.takeIf { it.isNotBlank() },
            )

        fun fromArgs(args: PeripheralDetailArgs): PeripheralDetailRequest =
            fromRaw(args.address, args.name)
    }
}

sealed interface PeripheralDetailEvent {
    object Save : PeripheralDetailEvent
    object Forget : PeripheralDetailEvent
    object Protect : PeripheralDetailEvent
    object Unprotect : PeripheralDetailEvent
    object RetryManual : PeripheralDetailEvent
    object ConnectHid : PeripheralDetailEvent
    object DisconnectHid : PeripheralDetailEvent
}

class PeripheralDetailViewModel(
    private val inventoryRepository: BluetoothDeviceInventoryRepository,
    private val hidHostController: HidHostController,
    private val policyStore: PeripheralPolicyStore,
    private val request: PeripheralDetailRequest,
) : AppViewModel() {

    private val address = request.address

    private val _uiEvents = MutableSharedFlow<UiEvents>()
    override val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    private val policyMutex = Mutex()
    private val busyMutex = Mutex()
    private var busyOperations = 0
    private var activeHidJob: kotlinx.coroutines.Job? = null

    val state: StateFlow<PeripheralDetailState> = combine(
        inventoryRepository.devices,
        policyStore.policy,
        hidHostController.profileStates,
        _isBusy
    ) { devices, policy, hidStates, busy ->
        val addr = address ?: return@combine PeripheralDetailState(isBusy = busy, isAddressValid = false)
        val device = devices.find { it.address == addr }
        val hidState = hidStates[addr] ?: ProfileConnectionState.DISCONNECTED

        val updatedDevice = device?.let {
            it.copy(profileStates = it.profileStates + (BluetoothProfileRole.HID_HOST to hidState))
        }

        PeripheralDetailState(
            device = updatedDevice,
            savedRecord = policy.savedPeripherals.find { it.address == addr },
            protectedRecord = policy.protectedDevices.find { it.address == addr },
            retryState = policy.retryStates[addr],
            isBusy = busy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeripheralDetailState())

    fun onEvent(event: PeripheralDetailEvent) {
        val addr = address
        if (addr == null) {
            viewModelScope.launch { _uiEvents.emit(UiEvents.ShowSnackBar("Invalid Bluetooth address")) }
            return
        }

        when (event) {
            PeripheralDetailEvent.Save,
            PeripheralDetailEvent.Forget,
            PeripheralDetailEvent.Protect,
            PeripheralDetailEvent.Unprotect -> runPolicyEvent(addr, event)
            PeripheralDetailEvent.RetryManual -> viewModelScope.launch {
                _uiEvents.emit(UiEvents.ShowToast("Manual retry requested"))
            }
            PeripheralDetailEvent.ConnectHid -> runHidEvent { hidHostController.connect(addr) }
            PeripheralDetailEvent.DisconnectHid -> runHidEvent { hidHostController.disconnect(addr) }
        }
    }

    private fun runPolicyEvent(addr: BluetoothAddress, event: PeripheralDetailEvent) {
        viewModelScope.launch {
            policyMutex.withLock {
                beginBusy()
                try {
                    when (event) {
                        PeripheralDetailEvent.Save -> {
                            policyStore.savePeripheral(
                                SavedPeripheralRecord(
                                    address = addr,
                                    displayName = displayNameFallback(),
                                    policy = ReconnectPolicy(),
                                    savedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }
                        PeripheralDetailEvent.Forget -> {
                            inventoryRepository.forgetSelected(addr).onSuccess {
                                policyStore.removeSavedPeripheral(addr)
                                _uiEvents.emit(UiEvents.NavigateBack)
                            }
                        }
                        PeripheralDetailEvent.Protect -> {
                            policyStore.protectDevice(
                                ProtectedPeripheralRecord(
                                    address = addr,
                                    displayName = displayNameFallback(),
                                    reason = "User protected",
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }
                        PeripheralDetailEvent.Unprotect -> policyStore.removeProtectedDevice(addr)
                        else -> Unit
                    }
                } finally {
                    endBusy()
                }
            }
        }
    }

    private fun runHidEvent(operation: suspend () -> HidOperationResult) {
        if (activeHidJob?.isActive == true) {
            viewModelScope.launch { _uiEvents.emit(UiEvents.ShowToast("HID operation already running")) }
            return
        }
        activeHidJob = viewModelScope.launch {
            beginBusy()
            try {
                handleHidResult(operation())
            } finally {
                activeHidJob = null
                endBusy()
            }
        }
    }

    private suspend fun beginBusy() {
        busyMutex.withLock {
            busyOperations += 1
            _isBusy.value = true
        }
    }

    private suspend fun endBusy() {
        busyMutex.withLock {
            busyOperations = (busyOperations - 1).coerceAtLeast(0)
            _isBusy.value = busyOperations > 0
        }
    }

    private fun displayNameFallback(): String =
        state.value.device?.displayName ?: request.fallbackName ?: UNKNOWN_DEVICE_NAME

    private suspend fun handleHidResult(result: HidOperationResult) {
        val msg = when (result) {
            is HidOperationResult.Failed -> "HID failed: ${result.reason}"
            HidOperationResult.NotAvailable -> "HID unavailable"
            HidOperationResult.RequiresDeviceValidation -> "HID validation needed"
            HidOperationResult.RequiresPrivilege -> "Privilege needed"
            HidOperationResult.Started -> "HID started"
        }
        _uiEvents.emit(UiEvents.ShowSnackBar(msg))
    }

    private companion object {
        const val UNKNOWN_DEVICE_NAME = "Unknown Device"
    }
}
