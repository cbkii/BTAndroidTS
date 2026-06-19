package com.cbkii.btandroidts.presentation.feature_connect.bt_profile.state

sealed interface BTProfileEvents {

	data object OnRetryFetchUUID : BTProfileEvents
}