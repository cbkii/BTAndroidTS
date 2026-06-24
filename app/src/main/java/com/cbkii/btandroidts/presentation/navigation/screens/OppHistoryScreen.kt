package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.domain.peripheral.OppTransferState
import com.cbkii.btandroidts.presentation.feature_opp.OppHistoryViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun OppHistoryScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = koinViewModel<OppHistoryViewModel>()
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { DateFormat.getDateTimeInstance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer History") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(end = 55.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text("Outbound transfers delegated to stock Android Bluetooth.", style = MaterialTheme.typography.bodySmall)
            }

            items(state.history) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transfer ${item.id.take(8)}", style = MaterialTheme.typography.titleSmall)
                            Text(dateFormat.format(Date(item.createdAtMillis)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${item.state}", style = MaterialTheme.typography.bodySmall, color = if (item.state == OppTransferState.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
