package com.cbkii.btandroidts.presentation.feature_le_server.state

import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEServerServices
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

data class BLEServerScreenState(
	val showServerRunningDialog: Boolean = false,
	val serverServices: ImmutableSet<BLEServerServices> = persistentSetOf(),
	val showServiceSelector: Boolean = false,
	val isServerRunning: Boolean = false,
)
