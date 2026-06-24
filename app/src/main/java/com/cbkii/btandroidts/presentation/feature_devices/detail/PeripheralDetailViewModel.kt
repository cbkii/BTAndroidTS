package com.cbkii.btandroidts.presentation.feature_devices.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.*
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import com.cbkii.btandroidts.presentation.util.AppViewModel
import com.cbkii.btandroidts.presentation.util.UiEvents
import com.ramcosta.composedestinations.generated.navArgs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PeripheralDetailState(
    val device: UnifiedBluetoothDevice? = null,
    val savedRecord: SavedPeripheralRecord? = null,
    val protectedRecord: ProtectedPeripheralRecord? = null,
    val retryState: PeripheralRetryState? = null,
    val isBusy: Boolean = false
)

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
    private val savedStateHandle: SavedStateHandle
) : AppViewModel() {

    private val args: PeripheralDetailArgs? = savedStateHandle.navArgs()
    private val address = args?.address?.let { BluetoothAddress.parse(it) }

    private val _uiEvents = MutableSharedFlow<UiEvents>()
    override val uiEvents: SharedFlow<UiEvents> = _uiEvents.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    private val eventMutex = Mutex()

    val state: StateFlow<PeripheralDetailState> = combine(
        inventoryRepository.devices,
        policyStore.policy,
        hidHostController.profileStates,
        _isBusy
    ) { devices, policy, hidStates, busy ->
        val addr = address ?: return@combine PeripheralDetailState()
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
        val addr = address ?: return
        viewModelScope.launch {
            eventMutex.withLock {
                _isBusy.value = true
                try {
                    when (event) {
                    PeripheralDetailEvent.Save -> {
                        policyStore.savePeripheral(
                            SavedPeripheralRecord(
                                address = addr,
                                displayName = displayNameForPolicy(),
                                policy = ReconnectPolicy(),
                                savedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    PeripheralDetailEvent.Forget -> {
                        inventoryRepository.forgetSelected(addr)
                            .onSuccess {
                                policyStore.removeSavedPeripheral(addr)
                                _uiEvents.emit(UiEvents.NavigateBack)
                            }
                            .onFailure { error ->
                                Log.w(TAG, "System unpair failed; removing saved peripheral policy", error)
                                policyStore.removeSavedPeripheral(addr)
                                _uiEvents.emit(UiEvents.ShowSnackBar("System unpair failed, but the saved peripheral record was removed."))
                                _uiEvents.emit(UiEvents.NavigateBack)
                            }
                    }
                    PeripheralDetailEvent.Protect -> {
                        policyStore.protectDevice(
                            ProtectedPeripheralRecord(
                                address = addr,
                                displayName = displayNameForPolicy(),
                                reason = "User protected",
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    PeripheralDetailEvent.Unprotect -> policyStore.removeProtectedDevice(addr)
                    PeripheralDetailEvent.RetryManual -> _uiEvents.emit(UiEvents.ShowToast("Manual retry requested"))
                    PeripheralDetailEvent.ConnectHid -> handleHidResult(runHidOperation("connect") { hidHostController.connect(addr) })
                    PeripheralDetailEvent.DisconnectHid -> handleHidResult(runHidOperation("disconnect") { hidHostController.disconnect(addr) })
                    }
                } finally {
                    _isBusy.value = false
                }
            }
        }
    }

    private fun displayNameForPolicy(): String = state.value.device?.displayName ?: args?.name ?: "Unknown Device"

    private suspend fun runHidOperation(
        operationName: String,
        operation: suspend () -> HidOperationResult,
    ): HidOperationResult = runCatching { operation() }
        .getOrElse { error ->
            Log.e(TAG, "HID $operationName operation failed before returning a result", error)
            HidOperationResult.Failed("HID $operationName failed on this TS18 build; collect diagnostics for validation")
        }

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

    companion object {
        private const val TAG = "PeripheralDetailViewModel"
    }
}
