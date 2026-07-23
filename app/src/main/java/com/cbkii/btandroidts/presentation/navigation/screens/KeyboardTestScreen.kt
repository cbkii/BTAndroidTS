package com.cbkii.btandroidts.presentation.navigation.screens

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val navBarWidth = dimensionResource(R.dimen.ts18_nav_bar_width)

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.keyboard_test_title)) }, navigationIcon = { IconButton(onClick = { navigator.popBackStack() }) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) } }) }) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(end = navBarWidth), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.EditText(ctx).apply {
                            hint = ctx.getString(R.string.keyboard_test_type_here)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            post { requestFocus() }
                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    if (!s.isNullOrEmpty()) viewModel.recordSuccessOnce()
                                }
                            })
                            setOnKeyListener { _, _, event ->
                                if (event.action == android.view.KeyEvent.ACTION_DOWN) viewModel.recordSuccessOnce()
                                false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                state.message?.let { message ->
                    Text(
                        text = message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS))
                    } catch (keyboardSettingsError: Exception) {
                        Log.w(KEYBOARD_TEST_TAG, "Unable to open hard keyboard settings", keyboardSettingsError)
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (settingsError: Exception) {
                            Log.e(KEYBOARD_TEST_TAG, "Unable to open Android settings fallback", settingsError)
                        }
                    }
                }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_tooltip_text))
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.keyboard_test_detected_devices), style = MaterialTheme.typography.titleMedium)
                state.inputDevices.forEach { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(device.name, style = MaterialTheme.typography.bodyMedium)
                            val deviceType = stringResource(
                                if (device.isKeyboard) R.string.keyboard_test_type_keyboard
                                else R.string.keyboard_test_type_other
                            )
                            Text(
                                stringResource(R.string.keyboard_test_device_type, deviceType),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardTestMessage.asString(): String = when (this) {
    KeyboardTestMessage.RefreshFailed -> stringResource(R.string.keyboard_test_error_refresh)
    is KeyboardTestMessage.Verified -> stringResource(R.string.keyboard_test_success_verified, address)
    KeyboardTestMessage.NoBondedMatch -> stringResource(R.string.keyboard_test_error_no_match)
    KeyboardTestMessage.MultipleBondedMatches -> stringResource(R.string.keyboard_test_error_multiple_match)
    KeyboardTestMessage.PersistenceFailed -> stringResource(R.string.keyboard_test_error_persist_failed)
}

sealed interface KeyboardTestMessage {
    data object RefreshFailed : KeyboardTestMessage
    data class Verified(val address: String) : KeyboardTestMessage
    data object NoBondedMatch : KeyboardTestMessage
    data object MultipleBondedMatches : KeyboardTestMessage
    data object PersistenceFailed : KeyboardTestMessage
}

data class KeyboardTestState(
    val inputDevices: List<AndroidInputDeviceInfo> = emptyList(),
    val verificationInProgress: Boolean = false,
    val verificationAttempted: Boolean = false,
    val verifiedAddress: BluetoothAddress? = null,
    val message: KeyboardTestMessage? = null,
)

class KeyboardTestViewModel(private val inputDeviceRepository: InputDeviceRepository, private val inventoryRepository: BluetoothDeviceInventoryRepository) : ViewModel() {
    private val _state = MutableStateFlow(KeyboardTestState())
    val state: StateFlow<KeyboardTestState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refreshInputDevices()
        viewModelScope.launch {
            inventoryRepository.devices.collect { refreshInputDevices() }
        }
    }

    fun refreshInputDevices() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val inputs = withContext(Dispatchers.Default) { inputDeviceRepository.listInputDevices() }
                val current = _state.value
                _state.value = current.copy(
                    inputDevices = inputs,
                    message = current.message.takeUnless { it == KeyboardTestMessage.RefreshFailed },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = KeyboardTestMessage.RefreshFailed)
                logKeyboardTestError("Failed to refresh input devices", e)
            }
        }
    }

    fun recordSuccessOnce() {
        val current = _state.value
        if (current.verificationInProgress || current.verificationAttempted || current.verifiedAddress != null) return
        _state.value = current.copy(verificationInProgress = true, message = null)
        viewModelScope.launch {
            try {
                val inputs = withContext(Dispatchers.Default) { inputDeviceRepository.listInputDevices() }
                _state.value = _state.value.copy(inputDevices = inputs)
                val matches = withContext(Dispatchers.Default) {
                    KeyboardInputVerifier.matchingBondedDevices(inventoryRepository.devices.value, inputs)
                }
                when (matches.size) {
                    1 -> {
                        val address = matches.single().address
                        val existing = inputDeviceRepository.getVerificationResult(address).first()
                        if (existing?.success != true) {
                            inputDeviceRepository.recordVerification(address, true)
                        }
                        _state.value = _state.value.copy(
                            verifiedAddress = address,
                            verificationAttempted = true,
                            message = KeyboardTestMessage.Verified(address.value),
                        )
                    }
                    0 -> _state.value = _state.value.copy(
                        verificationAttempted = false,
                        message = KeyboardTestMessage.NoBondedMatch,
                    )
                    else -> _state.value = _state.value.copy(
                        verificationAttempted = false,
                        message = KeyboardTestMessage.MultipleBondedMatches,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    verificationAttempted = false,
                    message = KeyboardTestMessage.PersistenceFailed,
                )
                logKeyboardTestError("Failed to persist keyboard verification", e)
            } finally {
                _state.value = _state.value.copy(verificationInProgress = false)
            }
        }
    }
}

private fun logKeyboardTestError(message: String, throwable: Throwable) {
    try {
        Log.e(KEYBOARD_TEST_TAG, message, throwable)
    } catch (_: RuntimeException) {
        // Local JVM tests use android.jar stubs; state reporting must not depend on logging.
    }
}

private const val KEYBOARD_TEST_TAG = "KeyboardTest"
