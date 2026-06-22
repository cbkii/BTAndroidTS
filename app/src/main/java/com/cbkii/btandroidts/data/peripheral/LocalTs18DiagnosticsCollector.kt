package com.cbkii.btandroidts.data.peripheral

import android.content.Context
import android.os.Build
import com.cbkii.btandroidts.BuildConfig
import com.cbkii.btandroidts.domain.peripheral.BluetoothDeviceInventoryRepository
import com.cbkii.btandroidts.domain.peripheral.InputDeviceRepository
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicyStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import com.cbkii.btandroidts.domain.peripheral.RootBluetoothOperation
import com.cbkii.btandroidts.domain.peripheral.RootBroker
import com.cbkii.btandroidts.domain.peripheral.TopwayLaneAdapter
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsCollector
import com.cbkii.btandroidts.domain.peripheral.Ts18DiagnosticsReport
import com.cbkii.btandroidts.domain.peripheral.VendorPackageInspector
import java.io.File

class LocalTs18DiagnosticsCollector(
	context: Context,
	private val inventoryRepository: BluetoothDeviceInventoryRepository,
	private val supervisor: PeripheralSupervisor,
	private val inputDeviceRepository: InputDeviceRepository,
	private val policyStore: PeripheralPolicyStore,
	private val vendorPackageInspector: VendorPackageInspector,
	private val topwayLaneAdapter: TopwayLaneAdapter,
	private val rootBroker: RootBroker,
) : Ts18DiagnosticsCollector {

	private val appContext = context.applicationContext

	override suspend fun collect(): Ts18DiagnosticsReport {
		val devices = inventoryRepository.devices.value
		val supervisorState = supervisor.state.value
		val inputDevices = inputDeviceRepository.listInputDevices()
		val policy = policyStore.currentPolicy()
		val vendorPackages = vendorPackageInspector.inspect().packages
		val topwayStatus = topwayLaneAdapter.phoneProjectionStatus()
		val rootState = rootBroker.run(RootBluetoothOperation.DumpBluetoothManager)
		val lines = buildList {
			add("BTAndroidTS diagnostics")
			add("generatedAt=${System.currentTimeMillis()}")
			add("app.version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
			add("app.id=${BuildConfig.APPLICATION_ID}")
			add("build.sdk=${Build.VERSION.SDK_INT}")
			add("build.release=${Build.VERSION.RELEASE}")
			add("build.display=${Build.DISPLAY}")
			add("build.fingerprint=${Build.FINGERPRINT}")
			add("boot.id=${readBootId() ?: "unavailable"}")
			add("package.filesDir=${appContext.filesDir.absolutePath}")
			add("inventory.count=${devices.size}")
			devices.forEach { device ->
				add(
					"device address=${redactAddress(device.address.value)} name=${device.displayName} " +
						"bond=${device.bondState} lane=${device.laneOwner} protection=${device.protectionStatus}"
				)
			}
			add("supervisor.enabled=${supervisorState.enabled}")
			add("supervisor.safeMode=${supervisorState.safeModeEnabled}")
			add("supervisor.saved=${supervisorState.savedPeripherals.size}")
			policy.savedPeripherals.forEach { saved ->
				add(
					"saved address=${redactAddress(saved.address.value)} name=${saved.displayName} " +
						"maxAttempts=${saved.policy.maxAttempts} last=${saved.lastResult.orEmpty()}"
				)
			}
			policy.protectedDevices.forEach { protected ->
				add("protected address=${redactAddress(protected.address.value)} reason=${protected.reason}")
			}
			policy.retryStates.forEach { (address, retry) ->
				add("retry address=${redactAddress(address.value)} attempt=${retry.attempt} next=${retry.nextAttemptAtMillis}")
			}
			add("input.count=${inputDevices.size}")
			inputDevices.forEach { input ->
				add("input id=${input.id} name=${input.name} keyboard=${input.isKeyboard} pointer=${input.isPointer}")
			}
			add("topway.bluetooth.present=${topwayStatus.topwayBluetoothPresent}")
			add("topway.bluetooth.enabled=${topwayStatus.topwayBluetoothEnabled}")
			add("android.bluetooth.present=${topwayStatus.androidBluetoothPresent}")
			topwayStatus.projectionPackages.forEach { pkg ->
				add("projection package=${pkg.packageName} enabled=${pkg.enabled} source=${pkg.source}")
			}
			vendorPackages.forEach { pkg ->
				add("vendor package=${pkg.packageName} family=${pkg.family} installed=${pkg.installed} enabled=${pkg.enabled} source=${pkg.source}")
			}
			add("root.operation=${rootState.operationName} status=${rootState.status} exit=${rootState.exitCode}")
			add("root.stderr=${rootState.stderr.take(160)}")
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

	private fun readBootId(): String? =
		runCatching { File("/proc/sys/kernel/random/boot_id").readText().trim().take(64) }.getOrNull()
}
