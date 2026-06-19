package com.cbkii.btandroidts.presentation.feature_le_connect.state

data class CharacteristicWriteDialogState(
	val showDialog: Boolean = false,
	val textFieldValue: String = "",
	val errorText: String? = null,
)