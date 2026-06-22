package com.cbkii.btandroidts.presentation.feature_connect.bt_client.state

import com.cbkii.btandroidts.domain.bluetooth.enums.ClientConnectionState
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel

data class BTClientDeviceState(
	val connectionStatus: ClientConnectionState = ClientConnectionState.CONNECTION_INITIALIZING,
	val device: BluetoothDeviceModel? = null
) {
	val isConnected: Boolean
		get() = connectionStatus == ClientConnectionState.CONNECTION_CONNECTED
}
