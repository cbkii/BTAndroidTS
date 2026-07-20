package com.cbkii.btandroidts.data.bluetooth_le

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import com.cbkii.btandroidts.data.mapper.canIndicate
import com.cbkii.btandroidts.data.mapper.canNotify
import com.cbkii.btandroidts.data.mapper.toDomainModel
import com.cbkii.btandroidts.data.samples.SampleUUIDReader
import com.cbkii.btandroidts.data.utils.hasBTConnectPermission
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.BluetoothLEClientConnector
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEConnectionState
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEConnectionEvents
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel
import com.cbkii.btandroidts.domain.exceptions.BLEIndicationOrNotifyRunningException
import com.cbkii.btandroidts.domain.exceptions.BLEMissingNotifyPropertiesException
import com.cbkii.btandroidts.domain.exceptions.BluetoothNotEnabled
import com.cbkii.btandroidts.domain.exceptions.BluetoothPermissionNotProvided
import com.cbkii.btandroidts.domain.exceptions.InvalidBLEConfigurationException
import com.cbkii.btandroidts.domain.exceptions.InvalidDeviceAddressException
import com.cbkii.btandroidts.domain.exceptions.InvalidMTUValueException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "BLE_CLIENT_LOGGER"
private const val CONNECTION_TIMEOUT_MS = 15_000L
private const val DISCONNECT_TIMEOUT_MS = 5_000L

@SuppressLint("MissingPermission")
class AndroidBLEClientConnector(
	private val context: Context,
	private val reader: SampleUUIDReader,
) : BluetoothLEClientConnector {

	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }
	private val bluetoothAdapter: BluetoothAdapter?
		get() = bluetoothManager?.adapter

	@Volatile
	private var operationQueue = GattOperationQueue(scope)

	private val gattCallback by lazy {
		BLEClientGattCallback(
			reader = reader,
			operationQueue = { operationQueue },
			onServiceChanged = {
				scope.launch {
					discoverServices().onFailure { error ->
						Log.w(TAG, "Service rediscovery failed", error)
					}
				}
			},
			scope = scope,
		)
	}

	private var transport: AndroidGattTransport? = null

	override val connectionState: StateFlow<BLEConnectionState>
		get() = gattCallback.connectionState

	override val connEvents: Flow<BLEConnectionEvents>
		get() = gattCallback.connEvents

	override val bleServices: Flow<List<BLEServiceModel>>
		get() = gattCallback.bleGattServices

	override val readForCharacteristic: Flow<BLECharacteristicsModel?>
		get() = gattCallback.readCharacteristics

	private var connectedDeviceValue: BluetoothDeviceModel? = null
	override val connectedDevice: BluetoothDeviceModel?
		get() = connectedDeviceValue

	private val notifyOrIndicationRunning = MutableStateFlow(false)
	override val isNotifyOrIndicationRunning: StateFlow<Boolean>
		get() = notifyOrIndicationRunning.asStateFlow()

	private var activeNotificationKey: GattAttributeKey? = null

	override suspend fun connect(address: String, autoConnect: Boolean): Result<Boolean> {
		if (!context.hasBTConnectPermission) {
			return Result.failure(BluetoothPermissionNotProvided())
		}
		if (bluetoothAdapter?.isEnabled != true) {
			return Result.failure(BluetoothNotEnabled())
		}
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			return Result.failure(InvalidDeviceAddressException())
		}

		return try {
			val device = bluetoothAdapter?.getRemoteDevice(address)
				?: return Result.failure(InvalidDeviceAddressException())
			connectedDeviceValue = device.toDomainModel()
			gattCallback.prepareForConnection()

			operationQueue.close(GattOperationException.SessionReplaced())
			operationQueue = GattOperationQueue(scope)
			transport?.close()

			@Suppress("DEPRECATION")
			val gatt = device.connectGatt(
				context,
				autoConnect,
				gattCallback,
				BluetoothDevice.TRANSPORT_LE,
			)
			val newTransport = AndroidGattTransport(gatt)
			transport = newTransport
			operationQueue.attachSession(newTransport.sessionToken)
			reader.loadFromFiles()

			val state = awaitConnectionTerminalState(CONNECTION_TIMEOUT_MS)
			if (state != BLEConnectionState.CONNECTED) {
				return Result.failure(
					IllegalStateException("Bluetooth GATT connection failed: $state")
				)
			}

			launchInitialOperations()
			Result.success(true)
		} catch (error: Exception) {
			if (error is CancellationException) throw error
			Result.failure(error)
		}
	}

	override suspend fun checkRssi(): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		return operationQueue.execute(
			name = "read remote RSSI",
			expectedCallback = ExpectedGattCallback(GattCallbackType.RSSI_READ),
			start = current::readRemoteRssi,
		).map { true }
	}

	override suspend fun discoverServices(): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		return operationQueue.execute(
			name = "discover services",
			expectedCallback = ExpectedGattCallback(GattCallbackType.SERVICES_DISCOVERED),
			start = current::discoverServices,
		).map { true }
	}

	override suspend fun reconnect(): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		return try {
			gattCallback.prepareForConnection()
			if (!current.reconnect()) {
				return Result.failure(GattOperationException.StartRejected("reconnect"))
			}
			val state = awaitConnectionTerminalState(CONNECTION_TIMEOUT_MS)
			if (state != BLEConnectionState.CONNECTED) {
				Result.failure(IllegalStateException("Bluetooth GATT reconnect failed: $state"))
			} else {
				launchInitialOperations()
				Result.success(true)
			}
		} catch (error: Exception) {
			if (error is CancellationException) throw error
			Result.failure(error)
		}
	}

	override suspend fun onUpdateMTU(mtu: Int): Result<Boolean> {
		if (mtu !in 23..517) return Result.failure(InvalidMTUValueException())
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		return operationQueue.execute(
			name = "request MTU $mtu",
			expectedCallback = ExpectedGattCallback(GattCallbackType.MTU_CHANGED),
			start = { current.requestMtu(mtu) },
		).map { true }
	}

	override suspend fun read(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
	): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		val gattCharacteristic = gattCallback.findCharacteristicFromDomainModel(service, characteristic)
			?: return Result.failure(InvalidBLEConfigurationException())
		val key = characteristicKey(service, characteristic)

		return operationQueue.execute(
			name = "read characteristic ${characteristic.uuid}",
			expectedCallback = ExpectedGattCallback(GattCallbackType.CHARACTERISTIC_READ, key),
			start = { current.readCharacteristic(gattCharacteristic) },
		).map { true }
	}

	override suspend fun write(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		value: String,
	): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		val gattCharacteristic = gattCallback.findCharacteristicFromDomainModel(service, characteristic)
			?: return Result.failure(InvalidBLEConfigurationException())
		val key = characteristicKey(service, characteristic)
		val bytes = value.encodeToByteArray()

		return operationQueue.execute(
			name = "write characteristic ${characteristic.uuid}",
			expectedCallback = ExpectedGattCallback(GattCallbackType.CHARACTERISTIC_WRITE, key),
			start = { current.writeCharacteristic(gattCharacteristic, bytes) },
		).map { true }
	}

	override suspend fun startIndicationOrNotification(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		enable: Boolean,
	): Result<Boolean> {
		if (!characteristic.isIndicateOrNotify) {
			return Result.failure(BLEMissingNotifyPropertiesException())
		}

		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		val gattCharacteristic = gattCallback.findCharacteristicFromDomainModel(service, characteristic)
			?: return Result.failure(InvalidBLEConfigurationException())
		val key = characteristicKey(service, characteristic)
		if (enable && activeNotificationKey != null && activeNotificationKey != key) {
			return Result.failure(BLEIndicationOrNotifyRunningException())
		}

		val descriptor = gattCharacteristic.getDescriptor(BLEClientUUID.CCC_DESCRIPTOR_UUID)
			?: return Result.failure(InvalidBLEConfigurationException())
		val descriptorValue = when {
			enable && characteristic.canNotify -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
			enable && characteristic.canIndicate -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
			!enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
			else -> return Result.failure(BLEMissingNotifyPropertiesException())
		}

		if (!current.setCharacteristicNotification(gattCharacteristic, enable)) {
			return Result.failure(
				GattOperationException.StartRejected("set local characteristic notification"),
			)
		}

		val writeResult = writeDescriptorInternal(
			descriptor = descriptor,
			key = descriptorKey(service, characteristic, descriptor.uuid),
			value = descriptorValue,
		)
		if (writeResult.isFailure) {
			current.setCharacteristicNotification(gattCharacteristic, !enable)
			return writeResult
		}

		activeNotificationKey = if (enable) key else null
		notifyOrIndicationRunning.value = enable
		return Result.success(true)
	}

	override suspend fun readDescriptor(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptor: BLEDescriptorModel,
	): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		val gattDescriptor = gattCallback.findDescriptorFromDomainModel(
			service = service,
			characteristic = characteristic,
			descriptor = descriptor,
		) ?: return Result.failure(InvalidBLEConfigurationException())

		return operationQueue.execute(
			name = "read descriptor ${descriptor.uuid}",
			expectedCallback = ExpectedGattCallback(
				type = GattCallbackType.DESCRIPTOR_READ,
				key = descriptorKey(service, characteristic, descriptor.uuid),
			),
			start = { current.readDescriptor(gattDescriptor) },
		).map { true }
	}

	override suspend fun writeToDescriptor(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptor: BLEDescriptorModel,
		value: String,
	): Result<Boolean> {
		val gattDescriptor = gattCallback.findDescriptorFromDomainModel(
			service = service,
			characteristic = characteristic,
			descriptor = descriptor,
		) ?: return Result.failure(InvalidBLEConfigurationException())
		return writeDescriptorInternal(
			descriptor = gattDescriptor,
			key = descriptorKey(service, characteristic, descriptor.uuid),
			value = value.encodeToByteArray(),
		)
	}

	override suspend fun disconnect(): Result<Unit> {
		val current = transport ?: return Result.success(Unit)
		return try {
			operationQueue.failActiveAndPending(GattOperationException.SessionDisconnected())
			activeNotificationKey = null
			notifyOrIndicationRunning.value = false
			current.disconnect()

			val state = awaitConnectionTerminalState(DISCONNECT_TIMEOUT_MS)
			if (state == BLEConnectionState.DISCONNECTED) {
				Result.success(Unit)
			} else {
				Result.failure(IllegalStateException("Bluetooth GATT disconnect failed: $state"))
			}
		} catch (error: Exception) {
			if (error is CancellationException) throw error
			Result.failure(error)
		}
	}

	override fun close() {
		operationQueue.close()
		activeNotificationKey = null
		notifyOrIndicationRunning.value = false
		connectedDeviceValue = null
		reader.clearCache()
		transport?.close()
		transport = null
		scope.cancel()
		Log.d(TAG, "GATT client closed")
	}

	private suspend fun writeDescriptorInternal(
		descriptor: BluetoothGattDescriptor,
		key: GattAttributeKey,
		value: ByteArray,
	): Result<Boolean> {
		val current = transport ?: return Result.failure(GattOperationException.NoActiveSession())
		return operationQueue.execute(
			name = "write descriptor ${descriptor.uuid}",
			expectedCallback = ExpectedGattCallback(GattCallbackType.DESCRIPTOR_WRITE, key),
			start = { current.writeDescriptor(descriptor, value) },
		).map { true }
	}

	private suspend fun awaitConnectionTerminalState(timeoutMs: Long): BLEConnectionState? {
		return withTimeoutOrNull(timeoutMs) {
			connectionState.first { state ->
				state == BLEConnectionState.CONNECTED ||
						state == BLEConnectionState.DISCONNECTED ||
						state == BLEConnectionState.FAILED
			}
		}
	}

	private fun launchInitialOperations() {
		scope.launch {
			checkRssi().onFailure { error -> Log.w(TAG, "Initial RSSI read failed", error) }
			discoverServices().onFailure { error -> Log.w(TAG, "Initial service discovery failed", error) }
		}
	}

	private fun characteristicKey(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
	) = GattAttributeKey(
		serviceUuid = service.serviceUUID,
		characteristicUuid = characteristic.uuid,
		instanceId = characteristic.instanceId,
	)

	private fun descriptorKey(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptorUuid: java.util.UUID,
	) = GattAttributeKey(
		serviceUuid = service.serviceUUID,
		characteristicUuid = characteristic.uuid,
		descriptorUuid = descriptorUuid,
		instanceId = characteristic.instanceId,
	)
}
