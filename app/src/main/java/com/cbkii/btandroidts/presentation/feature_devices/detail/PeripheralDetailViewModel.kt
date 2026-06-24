package com.cbkii.btandroidts.presentation.feature_devices.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.*
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import com.cbkii.btandroidts.presentation.util.AppViewModel
import com.cbkii.btandroidts.presentation.util.UiEvents
import com.ramcosta.composedestinations.generated.navArgs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
            _isBusy.value = true
            try {
                when (event) {
                    PeripheralDetailEvent.Save -> {
                        val device = state.value.device ?: return@launch
                        policyStore.savePeripheral(
                            SavedPeripheralRecord(
                                address = addr,
                                displayName = device.displayName,
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
                        val device = state.value.device ?: return@launch
                        policyStore.protectDevice(
                            ProtectedPeripheralRecord(
                                address = addr,
                                displayName = device.displayName,
                                reason = "User protected",
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    PeripheralDetailEvent.Unprotect -> policyStore.removeProtectedDevice(addr)
                    PeripheralDetailEvent.RetryManual -> _uiEvents.emit(UiEvents.ShowToast("Manual retry requested"))
                    PeripheralDetailEvent.ConnectHid -> handleHidResult(hidHostController.connect(addr))
                    PeripheralDetailEvent.DisconnectHid -> handleHidResult(hidHostController.disconnect(addr))
                }
            } finally {
                _isBusy.value = false
            }
        }
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
}
