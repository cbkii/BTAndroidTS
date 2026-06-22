package com.cbkii.btandroidts.presentation.feature_le_connect.state

interface CloseConnectionEvents {

	data object ShowCloseConnectionDialog : CloseConnectionEvents

	data object ConfirmCloseDialog : CloseConnectionEvents

	data object CancelCloseConnection : CloseConnectionEvents
}