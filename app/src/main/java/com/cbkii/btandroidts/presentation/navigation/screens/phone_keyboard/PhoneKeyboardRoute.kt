package com.cbkii.btandroidts.presentation.navigation.screens.phone_keyboard

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardCandidate
import com.cbkii.btandroidts.presentation.navigation.config.RouteAnimation
import com.cbkii.btandroidts.presentation.navigation.config.Routes
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.flow.collectLatest
import com.cbkii.btandroidts.presentation.util.LocalSnackBarProvider

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(route = Routes.PHONE_KEYBOARD_ROUTE, style = RouteAnimation::class)
@Composable
fun AnimatedVisibilityScope.PhoneKeyboardScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = koinViewModel<PhoneKeyboardViewModel>()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val snackBarHostState = LocalSnackBarProvider.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when(event) {
                is PhoneKeyboardUiEvent.NavigateToKeyboardTest -> navigator.navigate(com.ramcosta.composedestinations.generated.destinations.KeyboardTestDestination)
                is PhoneKeyboardUiEvent.ShowError -> {
                    val message = com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardGuide.getGuidanceText(event.reason)
                    snackBarHostState.showSnackbar(message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_mode_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = {
                        if(isScanning) viewModel.stopScan() else viewModel.startScan()
                    }) {
                        Text(if (isScanning) stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_stop_scan_btn) else stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_scan_btn))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            var showGuide by remember { mutableStateOf(false) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showGuide = !showGuide }) {
                    Text(if (showGuide) stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_hide_guide) else stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_show_guide))
                }
            }

            if (showGuide) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guide_title), style = MaterialTheme.typography.titleMedium)
                        com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardGuide.senderAppCompatibilityGuide.forEach { guide ->
                            Text("• ${guide.name}", style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guide_transport, guide.expectedTransport), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guide_setup, guide.setupInstructions), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (candidates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_scanning_text))
                        }
                    } else {
                        Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_tap_scan_text))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates) { candidate ->
                        CandidateCard(
                            candidate = candidate,
                            onPairClick = { viewModel.pairAndConnect(candidate) },
                            onVerifyClick = { viewModel.verifyInput(candidate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateCard(
    candidate: PhoneKeyboardCandidate,
    onPairClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = candidate.displayName ?: stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_unknown_device), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_card_transport, candidate.transport.name), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_card_bonded, candidate.isBonded), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_card_hid_profile, candidate.hidProfileState.name), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_card_input_node, candidate.inputVerificationState.name), style = MaterialTheme.typography.bodySmall)

            // Map the guidance action text
            val guidanceText = when(candidate.recommendedAction) {
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.OPEN_APP_ENABLE_ADVERTISING -> stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guidance_open_app)
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.PAIR_FROM_HOST -> stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guidance_pair)
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.VERIFY_INPUT_IN_TEST -> stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guidance_verify)
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.CONFLICT_WARNING -> stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_guidance_conflict)
               else -> candidate.recommendedAction.name
            }
            Text(text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_card_guidance, guidanceText), style = MaterialTheme.typography.bodySmall)

            if(candidate.lastFailureReason != null) {
               val failureText = com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardGuide.getGuidanceText(candidate.lastFailureReason)
               Text(text = stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_card_error, failureText), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPairClick,
                    enabled = !candidate.protectedTopwayRisk
                ) {
                    Text(if(candidate.isBonded) stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_btn_reconnect) else stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_btn_pair))
                }

                if (candidate.isBonded) {
                    OutlinedButton(
                        onClick = onVerifyClick
                    ) {
                        Text(stringResource(com.cbkii.btandroidts.R.string.phone_keyboard_btn_test))
                    }
                }
            }
        }
    }
}
