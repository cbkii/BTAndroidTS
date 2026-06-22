package com.cbkii.btandroidts.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cbkii.btandroidts.R

enum class BluetoothTypes(val tabIdx: Int) {
	CLASSIC(0),
	LOW_ENERGY(1)
}

val BluetoothTypes.textResource: String
	@Composable
	get() = when (this) {
		BluetoothTypes.CLASSIC -> stringResource(R.string.bluetooth_classic)
		BluetoothTypes.LOW_ENERGY -> stringResource(R.string.bluetooth_le)
	}
