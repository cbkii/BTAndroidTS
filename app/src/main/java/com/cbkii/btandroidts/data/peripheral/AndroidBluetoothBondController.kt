package com.cbkii.btandroidts.data.peripheral

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.cbkii.btandroidts.data.utils.hasBTConnectPermission
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.BluetoothBondController
import com.cbkii.btandroidts.domain.peripheral.BondFailureReason
import com.cbkii.btandroidts.domain.peripheral.BondingResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
class AndroidBluetoothBondController(
	context: Context,
) : BluetoothBondController {

	private val appContext = context.applicationContext
	private val bluetoothManager by lazy { appContext.getSystemService<BluetoothManager>() }
	private val bluetoothAdapter: BluetoothAdapter?
		get() = bluetoothManager?.adapter

	override suspend fun createBond(address: BluetoothAddress): BondingResult {
		if (!appContext.hasBTConnectPermission) {
			return BondingResult.Failed(BondFailureReason.PERMISSION_MISSING)
		}
		val device = getRemoteDevice(address)
			?: return BondingResult.Failed(BondFailureReason.ADAPTER_UNAVAILABLE)
		if (device.bondState == BluetoothDevice.BOND_BONDED) return BondingResult.AlreadyBonded

		return awaitBondTerminalState(address) {
			if (!device.createBond()) {
				trySend(BondTransition(BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_NONE, forcedFailure = true))
			}
		}
	}

	override suspend fun removeBond(address: BluetoothAddress): BondingResult {
		if (!appContext.hasBTConnectPermission) {
			return BondingResult.Failed(BondFailureReason.PERMISSION_MISSING)
		}
		val device = getRemoteDevice(address)
			?: return BondingResult.Failed(BondFailureReason.ADAPTER_UNAVAILABLE)
		if (device.bondState == BluetoothDevice.BOND_NONE) return BondingResult.Removed

		return awaitBondTerminalState(address) {
			val started = runCatching {
				val method = device.javaClass.getMethod("removeBond")
				method.invoke(device) as? Boolean ?: false
			}.getOrElse {
				trySend(
					BondTransition(
						current = BluetoothDevice.BOND_BONDED,
						previous = BluetoothDevice.BOND_BONDED,
						forcedFailure = true,
						detail = it.message,
					)
				)
				return@awaitBondTerminalState
			}
			if (!started) {
				trySend(BondTransition(BluetoothDevice.BOND_BONDED, BluetoothDevice.BOND_BONDED, forcedFailure = true))
			}
		}
	}

	private fun getRemoteDevice(address: BluetoothAddress): BluetoothDevice? =
		try {
			bluetoothAdapter?.getRemoteDevice(address.value)
		} catch (_: IllegalArgumentException) {
			null
		}

	private suspend fun awaitBondTerminalState(
		address: BluetoothAddress,
		trigger: suspend kotlinx.coroutines.channels.ProducerScope<BondTransition>.() -> Unit,
	): BondingResult {
		val transition = withTimeoutOrNull(BOND_TIMEOUT_MILLIS) {
			callbackFlow {
				val receiver = object : BroadcastReceiver() {
					override fun onReceive(context: Context?, intent: Intent?) {
						if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
						val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
						if (device?.address?.uppercase() != address.value) return
						trySend(
							BondTransition(
								current = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR),
								previous = intent.getIntExtra(
									BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
									BluetoothDevice.ERROR
								),
							)
						)
					}
				}
				ContextCompat.registerReceiver(
					appContext,
					receiver,
					IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
					ContextCompat.RECEIVER_EXPORTED
				)
				trigger()
				awaitClose { appContext.unregisterReceiver(receiver) }
			}.first { it.isTerminal }
		} ?: return BondingResult.Failed(BondFailureReason.TIMEOUT)

		if (transition.forcedFailure) {
			return BondingResult.Failed(BondFailureReason.START_FAILED, transition.detail)
		}

		return when (transition.current) {
			BluetoothDevice.BOND_BONDED -> BondingResult.Bonded
			BluetoothDevice.BOND_NONE -> {
				if (transition.previous == BluetoothDevice.BOND_BONDED) BondingResult.Removed
				else BondingResult.Failed(BondFailureReason.REMOTE_REJECTED)
			}
			else -> BondingResult.Failed(BondFailureReason.UNKNOWN)
		}
	}

	private data class BondTransition(
		val current: Int,
		val previous: Int,
		val forcedFailure: Boolean = false,
		val detail: String? = null,
	) {
		val isTerminal: Boolean
			get() = forcedFailure ||
				current == BluetoothDevice.BOND_BONDED ||
				current == BluetoothDevice.BOND_NONE
	}

	private companion object {
		const val BOND_TIMEOUT_MILLIS = 30_000L
	}
}
