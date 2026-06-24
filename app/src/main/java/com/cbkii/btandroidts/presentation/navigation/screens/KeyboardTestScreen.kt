package com.cbkii.btandroidts.presentation.navigation.screens

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.domain.peripheral.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun KeyboardTest(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<KeyboardTestViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.keyboard_test_title)) }, navigationIcon = { IconButton(onClick = { navigator.popBackStack() }) { Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(R.string.back_arrow)) } }) }) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(end = 55.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        val previousText = text
                        text = newText
                        if (previousText.isEmpty() && newText.isNotEmpty()) viewModel.recordSuccessOnce()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.keyboard_test_placeholder)) }
                )
                state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { openKeyboardSettings(context) }) { Text(stringResource(R.string.settings_tooltip_text)) }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.keyboard_test_detected_input_devices), style = MaterialTheme.typography.titleMedium)
                state.inputDevices.forEach { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(device.name, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.keyboard_test_device_type, if (device.isKeyboard) stringResource(R.string.keyboard_test_device_type_keyboard) else stringResource(R.string.keyboard_test_device_type_other)), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun openKeyboardSettings(context: android.content.Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS)) }
        .onFailure { keyboardSettingsError ->
            Log.w(TAG, "Unable to open hard keyboard settings; falling back to system settings", keyboardSettingsError)
            runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                .onFailure { fallbackError -> Log.w(TAG, "Unable to open fallback system settings", fallbackError) }
        }
}

data class KeyboardTestState(
    val inputDevices: List<AndroidInputDeviceInfo> = emptyList(),
    val message: String? = null,
)

class KeyboardTestViewModel(
    private val inputDeviceRepository: InputDeviceRepository,
    private val inventoryRepository: BluetoothDeviceInventoryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(KeyboardTestState())
    val state: StateFlow<KeyboardTestState> = _state.asStateFlow()

    private var verificationAttempted = false
    private var verifiedAddress: BluetoothAddress? = null
    private var cachedInputDevices: List<AndroidInputDeviceInfo>? = null

    init {
        refreshInputDevices()
    }

    fun recordSuccessOnce() {
        if (verificationAttempted || verifiedAddress != null) return
        verificationAttempted = true
        viewModelScope.launch {
            val inputDevices = cachedInputDevices ?: withContext(Dispatchers.Default) { inputDeviceRepository.listInputDevices() }
                .also { cachedInputDevices = it }
            val candidates = withContext(Dispatchers.Default) {
                KeyboardInputVerifier.matchingBondedDevices(inventoryRepository.devices.value, inputDevices)
            }
            when (candidates.size) {
                1 -> {
                    val candidate = candidates.single()
                    val address = candidate.address
                    val existingVerification = inputDeviceRepository.getVerificationResult(address).first()
                    verifiedAddress = address
                    if (existingVerification?.success != true) {
                        inputDeviceRepository.recordVerification(address, true)
                    }
                    _state.update { it.copy(message = "Input verified for ${candidate.displayName}") }
                }
                0 -> _state.update { it.copy(message = "No matching bonded input device found; verification not recorded.") }
                else -> _state.update { it.copy(message = "Multiple matching input devices found; choose one device before recording verification.") }
            }
        }
    }

    private fun refreshInputDevices() {
        viewModelScope.launch {
            val inputDevices = withContext(Dispatchers.Default) { inputDeviceRepository.listInputDevices() }
            cachedInputDevices = inputDevices
            _state.update { it.copy(inputDevices = inputDevices) }
        }
    }

}

internal object KeyboardInputVerifier {
    fun shouldStartVerification(previousText: String, newText: String, alreadyAttempted: Boolean, verifiedAddress: BluetoothAddress?): Boolean =
        !alreadyAttempted && verifiedAddress == null && previousText.isEmpty() && newText.isNotEmpty()

    fun matchingBondedDevices(
        devices: List<UnifiedBluetoothDevice>,
        inputDevices: List<AndroidInputDeviceInfo>,
    ): List<UnifiedBluetoothDevice> = devices.filter { device ->
        device.bondState == BondStatus.BONDED && inputDevices.any { it.matches(device.address) }
    }

    private fun AndroidInputDeviceInfo.matches(address: BluetoothAddress): Boolean {
        val compact = address.value.replace(":", "")
        return descriptor.contains(address.value, ignoreCase = true) ||
            descriptor.contains(compact, ignoreCase = true) ||
            name.contains(address.value, ignoreCase = true)
    }
}

private const val TAG = "KeyboardTestScreen"
