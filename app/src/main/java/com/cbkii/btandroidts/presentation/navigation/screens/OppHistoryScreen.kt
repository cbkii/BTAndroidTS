package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.annotation.StringRes
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
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.opp_history_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.back_arrow))
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
                Text(stringResource(R.string.opp_history_stock_bluetooth_note), style = MaterialTheme.typography.bodySmall)
            }

            items(state.history, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.opp_history_transfer_title, item.id.take(8)), style = MaterialTheme.typography.titleSmall)
                            Text(DateFormat.getDateTimeInstance().format(Date(item.createdAtMillis)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.opp_history_status, stringResource(oppTransferStateLabelRes(item.state))), style = MaterialTheme.typography.bodySmall, color = if (item.state == OppTransferState.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        if (item.state == OppTransferState.QUEUED || item.state == OppTransferState.DELEGATED_TO_STOCK_OPP || item.state == OppTransferState.RUNNING) {
                            TextButton(onClick = { viewModel.cancel(item.id) }) {
                                Text(stringResource(R.string.dialog_action_cancel))
                            }
                        }
                    }
                }
            }
        }
    }
}


@StringRes
private fun oppTransferStateLabelRes(state: OppTransferState): Int = when (state.name) {
    "QUEUED" -> R.string.opp_history_state_queued
    "RUNNING" -> R.string.opp_history_state_running
    "DELEGATED_TO_STOCK_OPP" -> R.string.opp_history_state_delegated_to_stock_opp
    "COMPLETED" -> R.string.opp_history_state_completed
    "FAILED" -> R.string.opp_history_state_failed
    "CANCELLED", "CANCELED" -> R.string.opp_history_state_cancelled
    else -> R.string.opp_history_state_unknown
}
