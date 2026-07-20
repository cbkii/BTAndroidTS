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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

	private val sessionGate = GattSessionGate()
	private val connectionMutex = Mutex()
	private val notificationMutex = Mutex()

	@Volatile
	private var operationQueue = newOperationQueue()

	private val gattCallback by lazy {
		BLEClientGattCallback(
			reader = reader,
			operationQueue = { operationQueue },
			sessionGate = sessionGate,
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
	private var lastAddress: String? = null
	private var lastAutoConnect: Boolean = false

	@Volatile
	private var disconnectRequested: Boolean = false

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
		return connectionMutex.withLock { connectInternal(address, autoConnect) }
	}

	private suspend fun connectInternal(address: String, autoConnect: Boolean): Result<Boolean> {
		if (!context.hasBTConnectPermission) return Result.failure(BluetoothPermissionNotProvided())
		if (bluetoothAdapter?.isEnabled != true) return Result.failure(BluetoothNotEnabled())
		if (!BluetoothAdapter.checkBluetoothAddress(address)) {
			return Result.failure(InvalidDeviceAddressException())
		}
		if (connectionState.value == BLEConnectionState.CONNECTED &&
			connectedDeviceValue?.address == address && connectedTransport() != null
		) return Result.success(true)

		disconnectRequested = false
		lastAddress = address
		lastAutoConnect = autoConnect
		var candidate: AndroidGattTransport? = null
		return try {
			val device = bluetoothAdapter?.getRemoteDevice(address)
				?: return Result.failure(InvalidDeviceAddressException())

			retireAndCloseTransport(transport)
			transport = null
			operationQueue.close(GattOperationException.SessionReplaced())
			operationQueue = newOperationQueue()
			gattCallback.prepareForConnection()
			connectedDeviceValue = device.toDomainModel()

			@Suppress("DEPRECATION")
			val gatt = device.connectGatt(
				context,
				autoConnect,
				gattCallback,
				BluetoothDevice.TRANSPORT_LE,
			)
			candidate = AndroidGattTransport(gatt)
			if (!sessionGate.activate(candidate.sessionToken)) {
				val failure = GattOperationException.SessionReplaced()
				releaseFailedConnection(candidate, failure)
				return Result.failure(failure)
			}

			transport = candidate
			operationQueue.attachSession(candidate.sessionToken)
			reader.loadFromFiles()

			val state = awaitConnectionTerminalState(CONNECTION_TIMEOUT_MS)
			if (state != BLEConnectionState.CONNECTED) {
				val failure = IllegalStateException("Bluetooth GATT connection failed: $state")
				releaseFailedConnection(candidate, failure)
				return Result.failure(failure)
			}

			launchInitialOperations()
			Result.success(true)
		} catch (error: Exception) {
			releaseFailedConnection(candidate ?: transport, error)
			if (error is CancellationException) throw error
			Result.failure(error)
		}
	}

	override suspend fun checkRssi(): Result<Boolean> {
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
		return operationQueue.execute(
			name = "read remote RSSI",
			expectedCallback = ExpectedGattCallback(GattCallbackType.RSSI_READ),
			start = current::readRemoteRssi,
		).map { true }
	}

	override suspend fun discoverServices(): Result<Boolean> {
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
		return operationQueue.execute(
			name = "discover services",
			expectedCallback = ExpectedGattCallback(GattCallbackType.SERVICES_DISCOVERED),
			start = current::discoverServices,
		).map { true }
	}

	override suspend fun reconnect(): Result<Boolean> {
		return connectionMutex.withLock {
			val address = lastAddress ?: connectedDeviceValue?.address
				?: return@withLock Result.failure(GattOperationException.NoActiveSession())
			connectInternal(address, lastAutoConnect)
		}
	}

	override suspend fun onUpdateMTU(mtu: Int): Result<Boolean> {
		if (mtu !in 23..517) return Result.failure(InvalidMTUValueException())
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
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
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
		val gattCharacteristic = gattCallback.findCharacteristicFromDomainModel(service, characteristic)
			?: return Result.failure(InvalidBLEConfigurationException())
		return operationQueue.execute(
			name = "read characteristic ${characteristic.uuid}",
			expectedCallback = ExpectedGattCallback(
				GattCallbackType.CHARACTERISTIC_READ,
				characteristicKey(service, characteristic),
			),
			start = { current.readCharacteristic(gattCharacteristic) },
		).map { true }
	}

	override suspend fun write(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		value: String,
	): Result<Boolean> {
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
		val gattCharacteristic = gattCallback.findCharacteristicFromDomainModel(service, characteristic)
			?: return Result.failure(InvalidBLEConfigurationException())
		return operationQueue.execute(
			name = "write characteristic ${characteristic.uuid}",
			expectedCallback = ExpectedGattCallback(
				GattCallbackType.CHARACTERISTIC_WRITE,
				characteristicKey(service, characteristic),
			),
			start = { current.writeCharacteristic(gattCharacteristic, value.encodeToByteArray()) },
		).map { true }
	}

	override suspend fun startIndicationOrNotification(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		enable: Boolean,
	): Result<Boolean> = notificationMutex.withLock {
		if (!characteristic.isIndicateOrNotify) {
			return@withLock Result.failure(BLEMissingNotifyPropertiesException())
		}

		val current = connectedTransport()
			?: return@withLock Result.failure(GattOperationException.SessionDisconnected())
		val gattCharacteristic = gattCallback.findCharacteristicFromDomainModel(service, characteristic)
			?: return@withLock Result.failure(InvalidBLEConfigurationException())
		val key = characteristicKey(service, characteristic)
		val activeKey = activeNotificationKey

		if (enable && activeKey == key) return@withLock Result.success(true)
		if (!enable && activeKey == null) return@withLock Result.success(true)
		if (activeKey != null && activeKey != key) {
			return@withLock Result.failure(BLEIndicationOrNotifyRunningException())
		}

		val descriptor = gattCharacteristic.getDescriptor(BLEClientUUID.CCC_DESCRIPTOR_UUID)
			?: return@withLock Result.failure(InvalidBLEConfigurationException())
		val descriptorValue = when {
			enable && characteristic.canNotify -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
			enable && characteristic.canIndicate -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
			!enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
			else -> return@withLock Result.failure(BLEMissingNotifyPropertiesException())
		}

		if (!current.setCharacteristicNotification(gattCharacteristic, enable)) {
			return@withLock Result.failure(
				GattOperationException.StartRejected("set local characteristic notification")
			)
		}

		val writeResult = writeDescriptorInternal(
			descriptor = descriptor,
			key = descriptorKey(service, characteristic, descriptor.uuid),
			value = descriptorValue,
		)
		if (writeResult.isFailure) {
			if (!disconnectRequested && sessionGate.isActive(current.sessionToken)) {
				runCatching {
					current.setCharacteristicNotification(gattCharacteristic, !enable)
				}.onFailure { error ->
					Log.w(TAG, "Failed to roll back local notification state", error)
				}
			}
			return@withLock writeResult
		}

		if (connectedTransport() !== current) {
			return@withLock Result.failure(GattOperationException.SessionDisconnected())
		}
		activeNotificationKey = if (enable) key else null
		notifyOrIndicationRunning.value = enable
		Result.success(true)
	}

	override suspend fun readDescriptor(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptor: BLEDescriptorModel,
	): Result<Boolean> {
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
		val gattDescriptor = gattCallback.findDescriptorFromDomainModel(
			service,
			characteristic,
			descriptor,
		) ?: return Result.failure(InvalidBLEConfigurationException())
		return operationQueue.execute(
			name = "read descriptor ${descriptor.uuid}",
			expectedCallback = ExpectedGattCallback(
				GattCallbackType.DESCRIPTOR_READ,
				descriptorKey(service, characteristic, descriptor.uuid),
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
		if (connectedTransport() == null) {
			return Result.failure(GattOperationException.SessionDisconnected())
		}
		val gattDescriptor = gattCallback.findDescriptorFromDomainModel(
			service,
			characteristic,
			descriptor,
		) ?: return Result.failure(InvalidBLEConfigurationException())
		return writeDescriptorInternal(
			descriptor = gattDescriptor,
			key = descriptorKey(service, characteristic, descriptor.uuid),
			value = value.encodeToByteArray(),
		)
	}

	override suspend fun disconnect(): Result<Unit> {
		return connectionMutex.withLock {
			val current = transport ?: return@withLock Result.success(Unit)
			if (connectionState.value == BLEConnectionState.DISCONNECTED) {
				finishDisconnectedSession(current)
				return@withLock Result.success(Unit)
			}

			disconnectRequested = true
			try {
				operationQueue.failActiveAndPending(GattOperationException.SessionDisconnected())
				activeNotificationKey = null
				notifyOrIndicationRunning.value = false
				current.disconnect()
				val state = awaitConnectionTerminalState(DISCONNECT_TIMEOUT_MS)
				if (state == BLEConnectionState.DISCONNECTED) {
					finishDisconnectedSession(current)
					Result.success(Unit)
				} else {
					val failure = IllegalStateException("Bluetooth GATT disconnect failed: $state")
					releaseFailedConnection(current, failure)
					Result.failure(failure)
				}
			} catch (error: Exception) {
				releaseFailedConnection(current, error)
				if (error is CancellationException) throw error
				Result.failure(error)
			}
		}
	}

	override fun close() {
		disconnectRequested = true
		operationQueue.close()
		activeNotificationKey = null
		notifyOrIndicationRunning.value = false
		connectedDeviceValue = null
		lastAddress = null
		reader.clearCache()
		retireAndCloseTransport(transport)
		transport = null
		scope.cancel()
		Log.d(TAG, "GATT client closed")
	}

	private suspend fun writeDescriptorInternal(
		descriptor: BluetoothGattDescriptor,
		key: GattAttributeKey,
		value: ByteArray,
	): Result<Boolean> {
		val current = connectedTransport()
			?: return Result.failure(GattOperationException.SessionDisconnected())
		return operationQueue.execute(
			name = "write descriptor ${descriptor.uuid}",
			expectedCallback = ExpectedGattCallback(GattCallbackType.DESCRIPTOR_WRITE, key),
			start = { current.writeDescriptor(descriptor, value) },
		).map { true }
	}

	private fun connectedTransport(): AndroidGattTransport? {
		return transport?.takeIf { current ->
			!disconnectRequested &&
				connectionState.value == BLEConnectionState.CONNECTED &&
				sessionGate.isActive(current.sessionToken)
		}
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

	private fun newOperationQueue(): GattOperationQueue {
		return GattOperationQueue(
			scope = scope,
			onSessionInvalidated = { sessionToken, cause ->
				scope.launch {
					connectionMutex.withLock {
						val current = transport
						if (current?.sessionToken === sessionToken) {
							releaseFailedConnection(current, cause)
						}
					}
				}
			},
		)
	}

	private fun finishDisconnectedSession(current: AndroidGattTransport) {
		operationQueue.close(GattOperationException.SessionDisconnected())
		retireAndCloseTransport(current)
		if (transport === current) transport = null
		disconnectRequested = false
		activeNotificationKey = null
		notifyOrIndicationRunning.value = false
	}

	private fun releaseFailedConnection(current: AndroidGattTransport?, cause: Throwable) {
		operationQueue.close(cause)
		gattCallback.markSessionFailed()
		retireAndCloseTransport(current)
		if (transport === current) transport = null
		disconnectRequested = false
		connectedDeviceValue = null
		activeNotificationKey = null
		notifyOrIndicationRunning.value = false
	}

	private fun retireAndCloseTransport(current: AndroidGattTransport?) {
		if (current == null) return
		sessionGate.retire(current.sessionToken)
		try {
			current.close()
		} catch (error: Exception) {
			Log.w(TAG, "Failed to close GATT transport", error)
		}
	}

	private fun characteristicKey(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
	) = GattAttributeKey(
		serviceUuid = service.serviceUUID,
		serviceInstanceId = service.serviceId,
		characteristicUuid = characteristic.uuid,
		instanceId = characteristic.instanceId,
	)

	private fun descriptorKey(
		service: BLEServiceModel,
		characteristic: BLECharacteristicsModel,
		descriptorUuid: java.util.UUID,
	) = GattAttributeKey(
		serviceUuid = service.serviceUUID,
		serviceInstanceId = service.serviceId,
		characteristicUuid = characteristic.uuid,
		descriptorUuid = descriptorUuid,
		instanceId = characteristic.instanceId,
	)
}
