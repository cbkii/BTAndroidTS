package com.cbkii.btandroidts.domain.peripheral

interface RootBroker {
	suspend fun run(operation: RootBluetoothOperation): RootBrokerResult
}

sealed interface RootBluetoothOperation {
	data object DumpBluetoothManager : RootBluetoothOperation
	data class ReadInputDevices(val address: BluetoothAddress?) : RootBluetoothOperation
	data object ReadTopwayLaneState : RootBluetoothOperation
}

data class RootBrokerResult(
	val status: RootBrokerStatus,
	val operationName: String,
	val stdout: String = "",
	val stderr: String = "",
	val exitCode: Int? = null,
	val startedAtMillis: Long,
	val finishedAtMillis: Long,
)

enum class RootBrokerStatus {
	DISABLED,
	SUCCESS,
	FAILED,
	TIMEOUT,
	NOT_ALLOWED,
}
