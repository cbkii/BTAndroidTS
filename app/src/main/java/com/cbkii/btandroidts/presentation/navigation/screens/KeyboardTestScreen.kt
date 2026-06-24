package com.cbkii.btandroidts.presentation.navigation.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun KeyboardTest(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<KeyboardTestViewModel>()
    val state by viewModel.state.collectAsState(KeyboardTestState())
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Keyboard Test") }, navigationIcon = { IconButton(onClick = { navigator.popBackStack() }) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) } }) }) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(end = 55.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it; if (it.isNotEmpty()) viewModel.recordSuccess() }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Type here...") })
                Button(onClick = { try { context.startActivity(Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS)) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }) { Text("Settings") }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Detected Input Devices", style = MaterialTheme.typography.titleMedium)
                state.inputDevices.forEach { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(device.name, style = MaterialTheme.typography.bodyMedium)
                            Text("Type: ${if (device.isKeyboard) "Keyboard" else "Other"}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

data class KeyboardTestState(val inputDevices: List<AndroidInputDeviceInfo> = emptyList())

class KeyboardTestViewModel(private val inputDeviceRepository: InputDeviceRepository, private val inventoryRepository: BluetoothDeviceInventoryRepository) : ViewModel() {
    val state: StateFlow<KeyboardTestState> = combine(inventoryRepository.devices) { _ -> KeyboardTestState(inputDeviceRepository.listInputDevices()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KeyboardTestState())
    fun recordSuccess() {
        viewModelScope.launch {
            inventoryRepository.devices.value.filter { it.bondState == BondStatus.BONDED }.forEach { if (inputDeviceRepository.hasInputDeviceFor(it.address)) inputDeviceRepository.recordVerification(it.address, true) }
        }
    }
}
