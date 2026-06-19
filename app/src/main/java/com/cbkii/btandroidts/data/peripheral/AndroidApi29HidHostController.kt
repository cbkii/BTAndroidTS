package com.cbkii.btandroidts.data.peripheral

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.HidHostController
import com.cbkii.btandroidts.domain.peripheral.HidOperationResult
import com.cbkii.btandroidts.domain.peripheral.ProfileConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
class AndroidApi29HidHostController(
	context: Context,
) : HidHostController {

	private val appContext = context.applicationContext
	private val bluetoothManager by lazy { appContext.getSystemService<BluetoothManager>() }
	private val bluetoothAdapter: BluetoothAdapter?
		get() = bluetoothManager?.adapter

	private val _profileStates = MutableStateFlow<Map<BluetoothAddress, ProfileConnectionState>>(emptyMap())
	override val profileStates: Flow<Map<BluetoothAddress, ProfileConnectionState>> = _profileStates.asStateFlow()

	override suspend fun connect(address: BluetoothAddress): HidOperationResult =
		invokeHidMethod(address, methodName = "connect")

	override suspend fun disconnect(address: BluetoothAddress): HidOperationResult =
		invokeHidMethod(address, methodName = "disconnect")

	override suspend fun setConnectionPolicy(address: BluetoothAddress, allowed: Boolean): HidOperationResult {
		val policyValue = if (allowed) CONNECTION_POLICY_ALLOWED else CONNECTION_POLICY_FORBIDDEN
		return withHidProxy { proxy, device ->
			val method = proxy.javaClass.methods.firstOrNull {
				it.name == "setConnectionPolicy" && it.isBluetoothDeviceIntMethod()
			} ?: proxy.javaClass.methods.firstOrNull {
				it.name == "setPriority" && it.isBluetoothDeviceIntMethod()
			} ?: return@withHidProxy HidOperationResult.Failed("HID policy method unavailable")

			val result = runCatching { method.invoke(proxy, device, policyValue) as? Boolean ?: false }
				.getOrElse { return@withHidProxy HidOperationResult.Failed(it.message ?: "HID policy failed") }
			if (result) HidOperationResult.Started else HidOperationResult.Failed("HID policy rejected")
		}(address)
	}

	private suspend fun invokeHidMethod(address: BluetoothAddress, methodName: String): HidOperationResult =
		withHidProxy { proxy, device ->
			val method = proxy.javaClass.methods.firstOrNull {
				it.name == methodName && it.parameterTypes.size == 1 &&
					it.parameterTypes[0] == BluetoothDevice::class.java
			} ?: return@withHidProxy HidOperationResult.Failed("HID $methodName method unavailable")

			val result = runCatching { method.invoke(proxy, device) as? Boolean ?: false }
				.getOrElse { return@withHidProxy HidOperationResult.Failed(it.message ?: "HID $methodName failed") }
			if (result) {
				_profileStates.value = _profileStates.value + (address to ProfileConnectionState.REQUIRES_DEVICE_VALIDATION)
				HidOperationResult.RequiresDeviceValidation
			} else {
				HidOperationResult.Failed("HID $methodName rejected")
			}
		}(address)

	private fun withHidProxy(
		block: suspend (BluetoothProfile, BluetoothDevice) -> HidOperationResult,
	): suspend (BluetoothAddress) -> HidOperationResult = operation@{ address ->
		if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_PRIVILEGED) !=
			PackageManager.PERMISSION_GRANTED
		) {
			return@operation HidOperationResult.RequiresPrivilege
		}
		val adapter = bluetoothAdapter ?: return@operation HidOperationResult.NotAvailable
		val device = runCatching { adapter.getRemoteDevice(address.value) }
			.getOrNull() ?: return@operation HidOperationResult.Failed("Invalid remote device")
		val proxy = getProfileProxy(adapter) ?: return@operation HidOperationResult.NotAvailable
		try {
			block(proxy, device)
		} finally {
			adapter.closeProfileProxy(HID_HOST_PROFILE_ID, proxy)
		}
	}

	private suspend fun getProfileProxy(adapter: BluetoothAdapter): BluetoothProfile? =
		withTimeoutOrNull(PROFILE_PROXY_TIMEOUT_MILLIS) {
			suspendCancellableCoroutine<BluetoothProfile?> { continuation ->
				val listener = object : BluetoothProfile.ServiceListener {
					override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
						if (profile == HID_HOST_PROFILE_ID && continuation.isActive) continuation.resume(proxy)
					}

					override fun onServiceDisconnected(profile: Int) {
						if (profile == HID_HOST_PROFILE_ID && continuation.isActive) continuation.resume(null)
					}
				}
				if (!adapter.getProfileProxy(appContext, listener, HID_HOST_PROFILE_ID)) {
					continuation.resume(null)
				}
			}
		}

	private companion object {
		const val HID_HOST_PROFILE_ID = 4
		const val CONNECTION_POLICY_FORBIDDEN = 0
		const val CONNECTION_POLICY_ALLOWED = 100
		const val PROFILE_PROXY_TIMEOUT_MILLIS = 5_000L
	}
}

private fun java.lang.reflect.Method.isBluetoothDeviceIntMethod(): Boolean =
	parameterTypes.size == 2 &&
		parameterTypes[0] == BluetoothDevice::class.java &&
		parameterTypes[1] == Int::class.javaPrimitiveType
