package com.cbkii.btandroidts.presentation.feature_connect.bt_client.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.domain.bluetooth.enums.ClientConnectionState
import com.cbkii.btandroidts.ui.theme.BTAndroidTSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BTClientTopBar(
	clientState: ClientConnectionState,
	onReconnect: () -> Unit,
	onDisconnect: () -> Unit,
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {},
	scrollBehavior: TopAppBarScrollBehavior? = null,
	colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
	MediumTopAppBar(
		title = { Text(text = stringResource(id = R.string.bt_client_route)) },
		navigationIcon = navigation,
		actions = {
			AnimatedConnectDisconnectButton(
				clientState = clientState,
				onConnect = onReconnect,
				onDisConnect = onDisconnect,
			)
		},
		colors = colors,
		scrollBehavior = scrollBehavior,
		modifier = modifier,
	)
}


private class ClientConnectionStatePreviewParams :
	CollectionPreviewParameterProvider<ClientConnectionState>(
		listOf(
			ClientConnectionState.CONNECTION_CONNECTED,
			ClientConnectionState.CONNECTION_DISCONNECTED,
			ClientConnectionState.CONNECTION_DENIED
		)
	)

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun AnimatedConnectDisconnectButtonPreview(
	@PreviewParameter(ClientConnectionStatePreviewParams::class)
	state: ClientConnectionState,
) = BTAndroidTSTheme {
	BTClientTopBar(
		clientState = state,
		onDisconnect = {},
		onReconnect = {},
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(id = R.string.back_arrow)
			)
		}
	)
}