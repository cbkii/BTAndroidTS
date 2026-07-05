package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PeripheralDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PeripheralManagerScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<PeripheralManagerViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ts18_dashboard_peripherals)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(id = R.string.back_arrow))
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
                    text = stringResource(R.string.peripheral_detail_title),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(state.pairedDevices, key = { it.address.value }) { device ->
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
