package com.cbkii.btandroidts.domain.bluetooth

import com.cbkii.btandroidts.domain.bluetooth.enums.PeerConnectionState
import com.cbkii.btandroidts.domain.bluetooth.enums.ServerConnectionState
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothServerConnector {

	/**
	 * Connection state for the server
	 * @see ServerConnectionState
	 */
	val serverState: StateFlow<ServerConnectionState>


	/**
	 * Connection state for the server
	 */
	val peerConnectionState: Flow<PeerConnectionState>

	/**
	 * The connected remote device
	 */
	val remoteDevice: Flow<BluetoothDeviceModel?>

	/**
	 * Starts the bluetooth classic server
	 * @param secure Indicates if the communication is rf comm secure or not
	 */
	suspend fun startServer(secure: Boolean = true)

	/**
	 * Reads the incomming data from the server
	 */
	val readIncomingData: Flow<BluetoothMessage>

	/**
	 * Sends data to the server
	 * @param data String to be sent
	 * @param trimData Whether to remove empty spaces around the string
	 * @return [Result] indicating if successfully send
	 */
	suspend fun sendData(data: String, trimData: Boolean = true): Result<Boolean>

	/**
	 * Finally close the server if not required any more
	 */
	fun closeServer()
}