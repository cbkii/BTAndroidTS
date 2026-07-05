package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.BondStatus
import com.cbkii.btandroidts.domain.peripheral.UnifiedBluetoothDevice
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PeripheralDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

data class PeripheralManagerState(
    val pairedDevices: List<UnifiedBluetoothDevice> = emptyList()
)

class PeripheralManagerViewModel(
    inventoryRepository: BluetoothDeviceInventoryRepository
) : ViewModel() {
    val state: StateFlow<PeripheralManagerState> = inventoryRepository.devices
        .map { devices ->
            PeripheralManagerState(
                pairedDevices = devices.filter { it.bondState == BondStatus.BONDED }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeripheralManagerState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PeripheralManagerScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<PeripheralManagerViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paired Peripherals") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(end = 55.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Select a bonded peripheral to manage connection, testing, and policy.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(state.pairedDevices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val args = PeripheralDetailArgs(device.address.value, device.displayName)
                            navigator.navigate(PeripheralDetailScreenDestination(args))
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = device.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(text = device.address.value, style = MaterialTheme.typography.bodySmall)
                        val types = device.transports.joinToString { it.name }
                        Text(text = "Transports: $types", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
