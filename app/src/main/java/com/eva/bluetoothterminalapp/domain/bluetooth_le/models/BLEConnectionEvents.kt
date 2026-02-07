package com.eva.bluetoothterminalapp.domain.bluetooth_le.models

import com.eva.bluetoothterminalapp.domain.bluetooth_le.enums.BLEPhysicalChannels

sealed class BLEConnectionEvents {
	data class OnRSSIUpdated(val rssi: Int) : BLEConnectionEvents()
	data class OnMTUUpdated(val mtu: Int) : BLEConnectionEvents()
	data class OnPhyUpdated(val tx: BLEPhysicalChannels, val rx: BLEPhysicalChannels) :
		BLEConnectionEvents()
}