package com.cbkii.btandroidts.data.peripheral

import android.content.Context
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisorScheduler

class AndroidPeripheralSupervisorScheduler(
	context: Context,
) : PeripheralSupervisorScheduler {

	private val appContext = context.applicationContext

	override fun requestManualRetry(reason: String): Result<Unit> =
		runCatching {
			PeripheralSupervisorService.start(
				context = appContext,
				action = PeripheralSupervisorService.ACTION_MANUAL_RETRY,
				reason = reason,
			)
		}

	override fun setSupervisionEnabled(enabled: Boolean, reason: String): Result<Unit> =
		runCatching {
			PeripheralSupervisorService.start(
				context = appContext,
				action = if (enabled) PeripheralSupervisorService.ACTION_ENABLE else PeripheralSupervisorService.ACTION_DISABLE,
				reason = reason,
			)
		}
}
