package com.cbkii.btandroidts.presentation.feature_connect.bt_client.state

import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothMessage
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class BTClientMessagesState(
	val messages: PersistentList<BluetoothMessage> = persistentListOf(),
	val textFieldValue: String = "",
)