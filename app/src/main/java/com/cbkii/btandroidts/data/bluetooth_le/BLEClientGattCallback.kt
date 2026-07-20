package com.cbkii.btandroidts.data.bluetooth_le

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import com.cbkii.btandroidts.data.mapper.toDomainModelWithName
import com.cbkii.btandroidts.data.mapper.toDomainModelWithNames
import com.cbkii.btandroidts.data.samples.SampleUUIDReader
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEConnectionState
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPhysicalChannels
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEConnectionEvents
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val GATT_LOGGER = "BLE_GATT_CALLBACK"

@SuppressLint("MissingPermission")
internal class BLEClientGattCallback(
	private val reader: SampleUUIDReader,
	private val operationQueue: () -> GattOperationQueue,
	private val sessionGate: GattSessionGate,
	private val onServiceChanged: () -> Unit,
	private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : BluetoothGattCallback() {

	private val _connectionState = MutableStateFlow(BLEConnectionState.CONNECTING)
	val connectionState = _connectionState.asStateFlow()

	private val _bleGattServices = MutableStateFlow<List<BluetoothGattService>>(emptyList())
	val bleGattServices = _bleGattServices.map { services ->
		services.toDomainModelWithNames(reader = reader)
	}

	private val bleGattServicesValue: List<BluetoothGattService>
		get() = _bleGattServices.value

	private val _readCharacteristic = MutableStateFlow<BLECharacteristicsModel?>(null)
	val readCharacteristics = _readCharacteristic.asStateFlow()

	private val _events = MutableSharedFlow<BLEConnectionEvents>(
		replay = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST,
	)
	val connEvents = _events.asSharedFlow()

	fun prepareForConnection() {
		_connectionState.value = BLEConnectionState.CONNECTING
		_bleGattServices.value = emptyList()
		_readCharacteristic.value = null
	}

	override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
		if (gatt == null || !acceptSession(gatt, "connection state")) return

		if (status != BluetoothGatt.GATT_SUCCESS) {
			operationQueue().failActiveAndPending(
				GattOperationException.CallbackFailed("connection", "status=$status"),
			)
			_connectionState.value = BLEConnectionState.FAILED
			Log.e(GATT_LOGGER, "Connection failed with status=$status")
			sessionGate.retire(gatt)
			gatt.close()
			return
		}

		val state = when (newState) {
			BluetoothProfile.STATE_CONNECTED -> BLEConnectionState.CONNECTED
			BluetoothProfile.STATE_DISCONNECTED -> BLEConnectionState.DISCONNECTED
			BluetoothProfile.STATE_CONNECTING -> BLEConnectionState.CONNECTING
			BluetoothProfile.STATE_DISCONNECTING -> BLEConnectionState.DISCONNECTING
			else -> BLEConnectionState.FAILED
		}
		_connectionState.value = state
		Log.d(GATT_LOGGER, "New connection state: $state")

		if (state == BLEConnectionState.DISCONNECTED || state == BLEConnectionState.FAILED) {
			operationQueue().failActiveAndPending(GattOperationException.SessionDisconnected())
		}
	}

	override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
		if (gatt == null || !acceptSession(gatt, "RSSI")) return
		if (status == BluetoothGatt.GATT_SUCCESS) {
			_events.tryEmit(BLEConnectionEvents.OnRSSIUpdated(rssi))
		}
		complete(gatt, GattCallbackType.RSSI_READ, status = status)
	}

	override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
		if (gatt == null || !acceptSession(gatt, "MTU")) return
		if (status == BluetoothGatt.GATT_SUCCESS) {
			_events.tryEmit(BLEConnectionEvents.OnMTUUpdated(mtu))
		}
		complete(gatt, GattCallbackType.MTU_CHANGED, status = status)
	}

	override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
		if (gatt == null || !acceptSession(gatt, "PHY read")) return
		if (status != BluetoothGatt.GATT_SUCCESS) return
		Log.d(GATT_LOGGER, "PHY read tx=$txPhy rx=$rxPhy")
	}

	override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
		if (gatt == null || !acceptSession(gatt, "PHY update")) return
		if (status != BluetoothGatt.GATT_SUCCESS) return
		_events.tryEmit(BLEConnectionEvents.OnPhyUpdated(txPhy.toBLPhy(), rxPhy.toBLPhy()))
	}

	override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
		if (gatt == null || !acceptSession(gatt, "service discovery")) return
		if (status == BluetoothGatt.GATT_SUCCESS) {
			_bleGattServices.value = gatt.services.orEmpty()
			Log.d(GATT_LOGGER, "Services discovered: ${gatt.services.size}")
		}
		complete(gatt, GattCallbackType.SERVICES_DISCOVERED, status = status)
	}

	override fun onServiceChanged(gatt: BluetoothGatt) {
		if (!acceptSession(gatt, "service changed")) return
		Log.d(GATT_LOGGER, "Services changed; queueing rediscovery")
		onServiceChanged()
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onCharacteristicRead(
		gatt: BluetoothGatt?,
		characteristic: BluetoothGattCharacteristic?,
		status: Int,
	) {
		if (gatt == null || characteristic == null) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
		onCharacteristicRead(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
	}

	override fun onCharacteristicRead(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray,
		status: Int,
	) {
		if (!acceptSession(gatt, "characteristic read")) return
		val key = characteristic.operationKey()
		if (status != BluetoothGatt.GATT_SUCCESS) {
			complete(gatt, GattCallbackType.CHARACTERISTIC_READ, status, key)
			return
		}

		scope.launch {
			try {
				val domainModel = characteristic.toDomainModelWithNames(reader).copy(byteArray = value)
				_readCharacteristic.update { previous ->
					if (previous?.uuid == domainModel.uuid &&
						previous.instanceId == domainModel.instanceId
					) {
						previous.copy(byteArray = value)
					} else {
						domainModel
					}
				}
				complete(gatt, GattCallbackType.CHARACTERISTIC_READ, status, key)
			} catch (error: Exception) {
				if (error is CancellationException) throw error
				Log.e(GATT_LOGGER, "Failed to map characteristic read", error)
				completeFailure(gatt, GattCallbackType.CHARACTERISTIC_READ, key, error)
			}
		}
	}

	override fun onCharacteristicWrite(
		gatt: BluetoothGatt?,
		characteristic: BluetoothGattCharacteristic?,
		status: Int,
	) {
		if (gatt == null || characteristic == null ||
			!acceptSession(gatt, "characteristic write")
		) return
		complete(
			gatt = gatt,
			type = GattCallbackType.CHARACTERISTIC_WRITE,
			key = characteristic.operationKey(),
			status = status,
		)
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onDescriptorRead(
		gatt: BluetoothGatt?,
		descriptor: BluetoothGattDescriptor?,
		status: Int,
	) {
		if (gatt == null || descriptor == null) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
		onDescriptorRead(gatt, descriptor, status, descriptor.value ?: byteArrayOf())
	}

	override fun onDescriptorRead(
		gatt: BluetoothGatt,
		descriptor: BluetoothGattDescriptor,
		status: Int,
		value: ByteArray,
	) {
		if (!acceptSession(gatt, "descriptor read")) return
		val key = descriptor.operationKey()
		if (status != BluetoothGatt.GATT_SUCCESS) {
			complete(gatt, GattCallbackType.DESCRIPTOR_READ, status, key)
			return
		}

		scope.launch {
			try {
				val characteristic = _readCharacteristic.value
				if (characteristic != null &&
					descriptor.characteristic.uuid == characteristic.uuid &&
					descriptor.characteristic.instanceId == characteristic.instanceId
				) {
					val domainModel = descriptor.toDomainModelWithName(reader).copy(byteArray = value)
					val descriptors = characteristic.descriptors.map { current ->
						if (current.uuid == domainModel.uuid) domainModel else current
					}
					_readCharacteristic.update { current ->
						current?.copy(descriptors = descriptors.toPersistentList())
					}
				}
				complete(gatt, GattCallbackType.DESCRIPTOR_READ, status, key)
			} catch (error: Exception) {
				if (error is CancellationException) throw error
				Log.e(GATT_LOGGER, "Failed to map descriptor read", error)
				completeFailure(gatt, GattCallbackType.DESCRIPTOR_READ, key, error)
			}
		}
	}

	override fun onDescriptorWrite(
		gatt: BluetoothGatt?,
		descriptor: BluetoothGattDescriptor?,
		status: Int,
	) {
		if (gatt == null || descriptor == null || !acceptSession(gatt, "descriptor write")) return
		complete(
			gatt = gatt,
			type = GattCallbackType.DESCRIPTOR_WRITE,
			key = descriptor.operationKey(),
			status = status,
		)
	}

	@Deprecated("Deprecated in Java")
	@Suppress("DEPRECATION")
	override fun onCharacteristicChanged(
		gatt: BluetoothGatt?,
		characteristic: BluetoothGattCharacteristic?,
	) {
		if (gatt == null || characteristic == null) return
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
		onCharacteristicChanged(gatt, characteristic, characteristic.value ?: byteArrayOf())
	}

	override fun onCharacteristicChanged(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray,
	) {
		if (!acceptSession(gatt, "characteristic changed")) return
		scope.launch {
			try {
				val current = _readCharacteristic.value
				if (current == null || current.uuid != characteristic.uuid ||
					current.instanceId != characteristic.instanceId
				) {
					_readCharacteristic.value = characteristic
						.toDomainModelWithNames(reader)
						.copy(byteArray = value)
				} else {
					_readCharacteristic.value = current.copy(byteArray = value)
				}
			} catch (error: Exception) {
				if (error is CancellationException) throw error
				Log.e(GATT_LOGGER, "Failed to map characteristic notification", error)
			}
		}
	}

	fun findCharacteristicFromDomainModel(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
	): BluetoothGattCharacteristic? {
		return bleGattServicesValue
			.find { candidate ->
				candidate.uuid == service.serviceUUID && candidate.instanceId == service.serviceId
			}
			?.characteristics
			?.find { candidate ->
				candidate.uuid == characteristic.uuid &&
						candidate.instanceId == characteristic.instanceId
			}
	}

	fun findDescriptorFromDomainModel(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptor: BLEDescriptorModel,
	): BluetoothGattDescriptor? {
		return findCharacteristicFromDomainModel(service, characteristic)
			?.getDescriptor(descriptor.uuid)
	}

	private fun acceptSession(gatt: BluetoothGatt, callback: String): Boolean {
		val accepted = sessionGate.activate(gatt)
		if (!accepted) Log.w(GATT_LOGGER, "Ignoring $callback callback from retired GATT session")
		return accepted
	}

	private fun complete(
		gatt: BluetoothGatt,
		type: GattCallbackType,
		status: Int,
		key: GattAttributeKey? = null,
	) {
		val accepted = operationQueue().onCallback(
			GattCallbackEvent(
				sessionToken = gatt,
				type = type,
				key = key,
				successful = status == BluetoothGatt.GATT_SUCCESS,
				detail = "status=$status",
			)
		)
		if (!accepted) {
			Log.w(GATT_LOGGER, "Ignoring unmatched callback type=$type key=$key status=$status")
		}
	}

	private fun completeFailure(
		gatt: BluetoothGatt,
		type: GattCallbackType,
		key: GattAttributeKey,
		error: Throwable,
	) {
		val accepted = operationQueue().onCallback(
			GattCallbackEvent(
				sessionToken = gatt,
				type = type,
				key = key,
				successful = false,
				detail = error.message ?: error::class.java.simpleName,
			)
		)
		if (!accepted) Log.w(GATT_LOGGER, "Ignoring unmatched failed callback type=$type key=$key")
	}

	private fun BluetoothGattCharacteristic.operationKey() = GattAttributeKey(
		serviceUuid = service?.uuid,
		serviceInstanceId = service?.instanceId,
		characteristicUuid = uuid,
		instanceId = instanceId,
	)

	private fun BluetoothGattDescriptor.operationKey() = GattAttributeKey(
		serviceUuid = characteristic.service?.uuid,
		serviceInstanceId = characteristic.service?.instanceId,
		characteristicUuid = characteristic.uuid,
		descriptorUuid = uuid,
		instanceId = characteristic.instanceId,
	)

	private fun Int.toBLPhy() = when (this) {
		BluetoothDevice.PHY_LE_CODED -> BLEPhysicalChannels.LE_CODED
		BluetoothDevice.PHY_LE_1M -> BLEPhysicalChannels.LE_1M
		BluetoothDevice.PHY_LE_2M -> BLEPhysicalChannels.LE_2M
		else -> throw IllegalArgumentException("Invalid transmission value: $this")
	}
}
