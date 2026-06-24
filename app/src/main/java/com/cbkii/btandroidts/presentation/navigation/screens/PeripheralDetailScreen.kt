package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.presentation.feature_devices.detail.PeripheralDetailEvent
import com.cbkii.btandroidts.presentation.feature_devices.detail.PeripheralDetailViewModel
import com.cbkii.btandroidts.presentation.navigation.UIEventsSideEffect
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import com.cbkii.btandroidts.presentation.navigation.config.RouteAnimation
import com.cbkii.btandroidts.presentation.util.LocalSharedTransitionVisibilityScopeProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel

@Destination<RootGraph>(
    style = RouteAnimation::class,
    navArgs = PeripheralDetailArgs::class
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedVisibilityScope.PeripheralDetailScreen(
    navigator: DestinationsNavigator,
    args: PeripheralDetailArgs
) {
    val viewModel = koinViewModel<PeripheralDetailViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    UIEventsSideEffect(
        events = { viewModel.uiEvents },
        onPopBack = dropUnlessResumed { navigator.popBackStack() }
    )

    CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.peripheral_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = dropUnlessResumed { navigator.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(top = 8.dp, end = 55.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.device?.let { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(device.displayName, style = MaterialTheme.typography.headlineSmall)
                            Text(device.address.value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.savedRecord == null) {
                        Button(onClick = { viewModel.onEvent(PeripheralDetailEvent.Save) }) { Text("Save") }
                    } else {
                        Button(onClick = { viewModel.onEvent(PeripheralDetailEvent.Forget) }) { Text("Forget") }
                    }
                    Button(onClick = {
                        if (state.protectedRecord == null) viewModel.onEvent(PeripheralDetailEvent.Protect)
                        else viewModel.onEvent(PeripheralDetailEvent.Unprotect)
                    }) { Text(if (state.protectedRecord == null) "Protect" else "Unprotect") }
                }
            }
        }
    }
}
