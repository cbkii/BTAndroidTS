package com.cbkii.btandroidts.presentation.feature_connect.bt_client.util

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.cbkii.btandroidts.domain.bluetooth.enums.ClientConnectionState
import com.cbkii.btandroidts.presentation.feature_connect.bt_client.state.BTClientDeviceState
import com.cbkii.btandroidts.presentation.util.PreviewFakes

class BTClientDeviceStatePreviewParam : CollectionPreviewParameterProvider<BTClientDeviceState>(
	listOf(
		BTClientDeviceState(),
		BTClientDeviceState(
			connectionStatus = ClientConnectionState.CONNECTION_CONNECTED,
			device = PreviewFakes.FAKE_DEVICE_MODEL
		),
		BTClientDeviceState(
			connectionStatus = ClientConnectionState.CONNECTION_DISCONNECTED,
			device = PreviewFakes.FAKE_DEVICE_MODEL
		)
	)
)