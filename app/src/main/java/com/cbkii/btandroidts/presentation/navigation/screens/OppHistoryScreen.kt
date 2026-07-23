package com.cbkii.btandroidts.presentation.navigation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.domain.peripheral.OppTransferState
import com.cbkii.btandroidts.presentation.feature_opp.OppHistoryViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun OppHistoryScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = koinViewModel<OppHistoryViewModel>()
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { DateFormat.getDateTimeInstance() }
    val context = LocalContext.current
    val errorMsgTemplate = stringResource(R.string.error_opp_transfer_failed)
    val successMsg = stringResource(R.string.opp_transfer_success_toast)
    val coroutineScope = rememberCoroutineScope()
    val navBarWidth = dimensionResource(R.dimen.ts18_nav_bar_width)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                viewModel.sendFile(it)
                    .onSuccess {
                        Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { err ->
                        val errorMsg = String.format(errorMsgTemplate, err.message ?: "Unknown error")
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch("*/*") },
                icon = { Icon(Icons.Filled.Send, contentDescription = null) },
                text = { Text(stringResource(R.string.action_send_file)) },
                modifier = Modifier.padding(end = navBarWidth)
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.opp_history_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(end = navBarWidth),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(stringResource(R.string.opp_history_outbound_desc), style = MaterialTheme.typography.bodySmall)
            }

            items(state.history) { item ->
                val stateLabel = stringResource(item.state.labelRes())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.opp_history_transfer, item.id.take(8)), style = MaterialTheme.typography.titleSmall)
                            Text(dateFormat.format(Date(item.createdAtMillis)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.opp_history_status, stateLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.state == OppTransferState.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
fun OppHistoryScreenPreview() {
    com.cbkii.btandroidts.ui.theme.BTAndroidTSTheme {
        val navBarWidth = dimensionResource(R.dimen.ts18_nav_bar_width)
        val completedLabel = stringResource(OppTransferState.COMPLETED.labelRes())

        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { },
                    icon = { Icon(Icons.Filled.Send, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_send_file)) },
                    modifier = Modifier.padding(end = navBarWidth)
                )
            },
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(stringResource(R.string.opp_history_title)) },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(end = navBarWidth),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(stringResource(R.string.opp_history_outbound_desc), style = MaterialTheme.typography.bodySmall)
                }

                items(3) { index ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.opp_history_transfer, "1a2b3c4d"), style = MaterialTheme.typography.titleSmall)
                                Text("Dec 31, 2024, 11:59:59 PM", style = MaterialTheme.typography.labelSmall)
                            }
                            Text("Summary of transfer $index", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.opp_history_status, completedLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@StringRes
private fun OppTransferState.labelRes(): Int = when (this) {
    OppTransferState.QUEUED -> R.string.opp_transfer_state_queued
    OppTransferState.DELEGATED_TO_STOCK_OPP -> R.string.opp_transfer_state_delegated_to_stock_opp
    OppTransferState.RUNNING -> R.string.opp_transfer_state_running
    OppTransferState.CANCELLED -> R.string.opp_transfer_state_cancelled
    OppTransferState.FAILED -> R.string.opp_transfer_state_failed
    OppTransferState.COMPLETED -> R.string.opp_transfer_state_completed
    OppTransferState.REQUIRES_DEVICE_VALIDATION -> R.string.opp_transfer_state_requires_device_validation
}
