package com.cbkii.btandroidts.data.peripheral

import com.cbkii.btandroidts.domain.peripheral.RootBluetoothOperation
import com.cbkii.btandroidts.domain.peripheral.RootBroker
import com.cbkii.btandroidts.domain.peripheral.RootBrokerResult
import com.cbkii.btandroidts.domain.peripheral.RootBrokerStatus

class DisabledRootBroker : RootBroker {
	override suspend fun run(operation: RootBluetoothOperation): RootBrokerResult {
		val now = System.currentTimeMillis()
		return RootBrokerResult(
			status = RootBrokerStatus.DISABLED,
			operationName = operation.javaClass.simpleName,
			stderr = "Root broker is disabled until a fixed operation is explicitly enabled and validated",
			startedAtMillis = now,
			finishedAtMillis = now,
		)
	}
}
