package com.cbkii.btandroidts.data.peripheral

import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsCollector
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsReport

class LocalTs18DiagnosticsCollector(
	private val inventoryRepository: BluetoothDeviceInventoryRepository,
	private val supervisor: PeripheralSupervisor,
	private val inputDeviceRepository: InputDeviceRepository,
) : Ts18DiagnosticsCollector {

	override suspend fun collect(): Ts18DiagnosticsReport {
		val devices = inventoryRepository.devices.value
		val supervisorState = supervisor.state.value
		val inputDevices = inputDeviceRepository.listInputDevices()
		val lines = buildList {
			add("BTAndroidTS diagnostics")
			add("generatedAt=${System.currentTimeMillis()}")
			add("inventory.count=${devices.size}")
			devices.forEach { device ->
				add(
					"device address=${redactAddress(device.address.value)} name=${device.displayName} " +
						"bond=${device.bondState} lane=${device.laneOwner} protection=${device.protectionStatus}"
				)
			}
			add("supervisor.enabled=${supervisorState.enabled}")
			add("supervisor.saved=${supervisorState.savedPeripherals.size}")
			add("input.count=${inputDevices.size}")
			inputDevices.forEach { input ->
				add("input id=${input.id} name=${input.name} keyboard=${input.isKeyboard} pointer=${input.isPointer}")
			}
			inventoryRepository.capabilities.value.forEach { capability ->
				add("capability ${capability.feature}=${capability.status} reason=${capability.reason}")
			}
		}
		return Ts18DiagnosticsReport(
			generatedAtMillis = System.currentTimeMillis(),
			summary = "devices=${devices.size}, saved=${supervisorState.savedPeripherals.size}, inputs=${inputDevices.size}",
			lines = lines,
		)
	}

	private fun redactAddress(address: String): String {
		val parts = address.split(":")
		if (parts.size != 6) return "<invalid>"
		return "${parts[0]}:${parts[1]}:xx:xx:xx:${parts[5]}"
	}
}
