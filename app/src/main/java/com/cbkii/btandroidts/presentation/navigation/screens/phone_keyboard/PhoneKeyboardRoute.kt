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

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(route = Routes.PHONE_KEYBOARD_ROUTE, style = RouteAnimation::class)
@Composable
fun AnimatedVisibilityScope.PhoneKeyboardScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = koinViewModel<PhoneKeyboardViewModel>()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Keyboard Mode") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = {
                        if(isScanning) viewModel.stopScan() else viewModel.startScan()
                    }) {
                        Text(if (isScanning) "Stop Scan" else "Scan")
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
            Text(
                text = "Instructions: Open Android Bluetooth Keyboard/HID app on phone, enable Bluetooth keyboard/server/HID mode, keep app foreground, then pair from here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (candidates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No candidates found.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates) { candidate ->
                        CandidateCard(
                            candidate = candidate,
                            onPairClick = { viewModel.pairAndConnect(candidate) },
                            onVerifyClick = { viewModel.verifyInput(candidate, navigator) }
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
            Text(text = candidate.displayName ?: "Unknown BLE device", style = MaterialTheme.typography.titleMedium)
            Text(text = "Transport: ${candidate.transport}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Bonded: ${candidate.isBonded}", style = MaterialTheme.typography.bodySmall)
            Text(text = "HID Profile: ${candidate.hidProfileState}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Input Node: ${candidate.inputVerificationState}", style = MaterialTheme.typography.bodySmall)

            // Map the guidance action text
            val guidanceText = when(candidate.recommendedAction) {
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.OPEN_APP_ENABLE_ADVERTISING -> "Open the Android Keyboard app and enable Bluetooth keyboard mode."
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.PAIR_FROM_HOST -> "Pair from the TS18 host."
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.VERIFY_INPUT_IN_TEST -> "Verify keys in Keyboard Test."
               com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardUserGuidance.CONFLICT_WARNING -> "This device is a Topway protected vendor device and shouldn't be paired."
               else -> candidate.recommendedAction.name
            }
            Text(text = "Guidance: $guidanceText", style = MaterialTheme.typography.bodySmall)

            if(candidate.lastFailureReason != null) {
               val failureText = com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardGuide.getGuidanceText(candidate.lastFailureReason)
               Text(text = "Error: $failureText", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPairClick,
                    enabled = !candidate.protectedTopwayRisk
                ) {
                    Text(if(candidate.isBonded) "Reconnect" else "Pair")
                }

                if (candidate.isBonded) {
                    OutlinedButton(
                        onClick = onVerifyClick
                    ) {
                        Text("Keyboard Test")
                    }
                }
            }
        }
    }
}
