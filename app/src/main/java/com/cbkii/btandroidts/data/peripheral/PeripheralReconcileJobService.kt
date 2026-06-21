package com.cbkii.btandroidts.data.peripheral

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import androidx.core.content.getSystemService
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicyStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.coroutineContext

class PeripheralReconcileJobService : JobService(), KoinComponent {

	private val supervisor: PeripheralSupervisor by inject()
	private val policyStore: PeripheralPolicyStore by inject()
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var activeJob: Job? = null

	override fun onStartJob(params: JobParameters): Boolean {
		activeJob?.cancel()
		activeJob = scope.launch {
			val reason = params.extras.getString(EXTRA_REASON) ?: "background reconciliation"
			try {
				supervisor.reconcile(reason)
				scheduleNextRetryFromPolicy()
				jobFinished(params, false)
			} catch (cancellation: CancellationException) {
				throw cancellation
			} catch (_: Throwable) {
				jobFinished(params, false)
			} finally {
				if (activeJob === coroutineContext[Job]) {
					activeJob = null
				}
			}
		}
		return true
	}

	override fun onStopJob(params: JobParameters): Boolean {
		activeJob?.cancel()
		activeJob = null
		return true
	}

	override fun onDestroy() {
		activeJob?.cancel()
		scope.cancel()
		super.onDestroy()
	}

	private suspend fun scheduleNextRetryFromPolicy() {
		val policy = policyStore.currentPolicy()
		if (!policy.supervisionEnabled || policy.safeModeEnabled) return
		val now = System.currentTimeMillis()
		val nextAt = policy.retryStates.values
			.map { it.nextAttemptAtMillis }
			.filter { it > now }
			.minOrNull()
			?: return
		schedule(this, "scheduled retry", nextAt - now)
	}

	companion object {
		private const val JOB_ID = 1803
		private const val EXTRA_REASON = "com.cbkii.btandroidts.extra.RECONCILE_REASON"
		private const val MAX_SCHEDULE_SLACK_MILLIS = 60_000L

		fun schedule(context: Context, reason: String, minLatencyMillis: Long = 0L) {
			val appContext = context.applicationContext
			val scheduler = appContext.getSystemService<JobScheduler>() ?: return
			val delay = minLatencyMillis.coerceAtLeast(0L)
			val jobInfo = JobInfo.Builder(
				JOB_ID,
				ComponentName(appContext, PeripheralReconcileJobService::class.java)
			)
				.setExtras(PersistableBundle().apply { putString(EXTRA_REASON, reason) })
				.setMinimumLatency(delay)
				.setOverrideDeadline(delay + MAX_SCHEDULE_SLACK_MILLIS)
				.build()
			scheduler.schedule(jobInfo)
		}

		fun cancel(context: Context) {
			context.applicationContext.getSystemService<JobScheduler>()?.cancel(JOB_ID)
		}
	}
}
