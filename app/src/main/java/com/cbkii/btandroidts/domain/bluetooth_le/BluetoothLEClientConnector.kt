package com.cbkii.btandroidts.domain.bluetooth_le

import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEConnectionState
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPropertyTypes
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEConnectionEvents
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothLEClientConnector {
	val connectionState: StateFlow<BLEConnectionState>
	val connEvents: Flow<BLEConnectionEvents>
	val bleServices: Flow<List<BLEServiceModel>>
	val connectedDevice: BluetoothDeviceModel?
	val readForCharacteristic: Flow<BLECharacteristicsModel?>
	val isNotifyOrIndicationRunning: StateFlow<Boolean>

	/** Connects and completes only after the connection callback succeeds or fails. */
	suspend fun connect(address: String, autoConnect: Boolean = false): Result<Boolean>

	/** Completes after the matching characteristic-read callback. */
	suspend fun read(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
	): Result<Boolean>

	/** Completes after the matching characteristic-write callback. */
	suspend fun write(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		value: String,
	): Result<Boolean>

	/** Completes after the matching descriptor-read callback. */
	suspend fun readDescriptor(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptor: BLEDescriptorModel,
	): Result<Boolean>

	/** Completes after the matching descriptor-write callback. */
	suspend fun writeToDescriptor(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptor: BLEDescriptorModel,
		value: String,
	): Result<Boolean>

	/**
	 * Enables or disables indication/notification and completes only after the CCCD write callback.
	 * The local active state is changed only after the remote descriptor write succeeds.
	 */
	suspend fun startIndicationOrNotification(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		enable: Boolean,
	): Result<Boolean>

	suspend fun onUpdateMTU(mtu: Int): Result<Boolean>
	suspend fun discoverServices(): Result<Boolean>
	suspend fun checkRssi(): Result<Boolean>
	suspend fun reconnect(): Result<Boolean>
	suspend fun disconnect(): Result<Unit>

	/** Permanently releases the connector and its session queue. */
	fun close()
}
