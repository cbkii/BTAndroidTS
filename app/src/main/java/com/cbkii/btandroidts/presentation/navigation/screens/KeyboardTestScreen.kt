package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.presentation.navigation.config.Routes
import com.cbkii.btandroidts.ui.theme.BTAndroidTSTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(
	route = Routes.KEYBOARD_TEST_ROUTE
)
@Composable
fun KeyboardTestScreen(
	navigator: DestinationsNavigator
) {
	var text by remember { mutableStateOf("") }
	var lastChar by remember { mutableStateOf("") }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.keyboard_test_title)) },
				navigationIcon = {
					IconButton(onClick = { navigator.popBackStack() }) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back_arrow)
						)
					}
				}
			)
		}
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Text(
				text = stringResource(R.string.keyboard_test_desc),
				style = MaterialTheme.typography.bodyLarge
			)

			OutlinedTextField(
				value = text,
				onValueChange = {
					text = it
					if (it.isNotEmpty()) {
						lastChar = it.last().toString()
					}
				},
				modifier = Modifier.fillMaxWidth(),
				placeholder = { Text(stringResource(R.string.keyboard_test_placeholder)) },
				label = { Text(stringResource(R.string.keyboard_test_title)) }
			)

			if (lastChar.isNotEmpty()) {
				Text(
					text = stringResource(R.string.keyboard_test_last_char, lastChar),
					style = MaterialTheme.typography.headlineMedium,
					color = MaterialTheme.colorScheme.primary
				)
			}
		}
	}
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun KeyboardTestScreenPreview() = BTAndroidTSTheme {
	KeyboardTestScreen(navigator = com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator)
}
