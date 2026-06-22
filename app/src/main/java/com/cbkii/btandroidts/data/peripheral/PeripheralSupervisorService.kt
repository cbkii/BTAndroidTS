package com.cbkii.btandroidts.data.peripheral

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicy
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicyStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralSupervisor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PeripheralSupervisorService : Service(), KoinComponent {

	private val supervisor: PeripheralSupervisor by inject()
	private val policyStore: PeripheralPolicyStore by inject()
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var reconcileJob: Job? = null

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onCreate() {
		super.onCreate()
		createNotificationChannel()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startAsForeground(intent?.getStringExtra(EXTRA_REASON) ?: "Supervisor reconciliation")
		reconcileJob?.cancel()
		reconcileJob = scope.launch {
			when (intent?.action ?: ACTION_RECONCILE) {
				ACTION_ENABLE -> policyStore.setSupervisionEnabled(true)
				ACTION_DISABLE -> {
					policyStore.setSupervisionEnabled(false)
					cancelScheduledRetry()
					stopServiceInstance(startId)
					return@launch
				}
				ACTION_MANUAL_RETRY -> clearRetryStatesForManualRetry()
			}
			val reason = intent?.getStringExtra(EXTRA_REASON) ?: intent?.action ?: "reconcile"
			supervisor.reconcile(reason)
			scheduleNextRetry(policyStore.currentPolicy())
			stopServiceInstance(startId)
		}
		return START_NOT_STICKY
	}

	override fun onDestroy() {
		reconcileJob?.cancel()
		scope.cancel()
		super.onDestroy()
	}

	private suspend fun clearRetryStatesForManualRetry() {
		policyStore.currentPolicy().savedPeripherals.forEach { saved ->
			policyStore.setRetryState(saved.address, null)
		}
	}

	private fun scheduleNextRetry(policy: PeripheralPolicy) {
		cancelScheduledRetry()
		if (!policy.supervisionEnabled || policy.safeModeEnabled) return
		val nextAt = policy.retryStates.values
			.map { it.nextAttemptAtMillis }
			.filter { it > System.currentTimeMillis() }
			.minOrNull()
			?: return
		PeripheralReconcileJobService.schedule(
			context = this,
			reason = "scheduled retry",
			minLatencyMillis = nextAt - System.currentTimeMillis()
		)
	}

	private fun cancelScheduledRetry() {
		PeripheralReconcileJobService.cancel(this)
	}

	private fun startAsForeground(reason: String) {
		val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setContentTitle(getString(R.string.supervisor_notification_title))
			.setContentText(reason)
			.setOngoing(false)
			.setLocalOnly(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.build()
		runCatching {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				startForeground(
					NOTIFICATION_ID,
					notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
				)
			} else {
				startForeground(NOTIFICATION_ID, notification)
			}
		}.onFailure {
			stopSelf()
		}
	}

	private fun stopServiceInstance(startId: Int) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			stopForeground(Service.STOP_FOREGROUND_REMOVE)
		} else {
			@Suppress("DEPRECATION")
			stopForeground(true)
		}
		stopSelf(startId)
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val notificationManager = getSystemService<NotificationManager>() ?: return
		val channel = NotificationChannel(
			NOTIFICATION_CHANNEL_ID,
			getString(R.string.supervisor_notification_channel),
			NotificationManager.IMPORTANCE_LOW
		).apply {
			description = getString(R.string.supervisor_notification_channel_desc)
			setShowBadge(false)
			lockscreenVisibility = Notification.VISIBILITY_PRIVATE
		}
		notificationManager.createNotificationChannel(channel)
	}

	companion object {
		const val ACTION_RECONCILE = "com.cbkii.btandroidts.action.RECONCILE_PERIPHERALS"
		const val ACTION_ENABLE = "com.cbkii.btandroidts.action.ENABLE_SUPERVISION"
		const val ACTION_DISABLE = "com.cbkii.btandroidts.action.DISABLE_SUPERVISION"
		const val ACTION_MANUAL_RETRY = "com.cbkii.btandroidts.action.MANUAL_RETRY"
		private const val EXTRA_REASON = "com.cbkii.btandroidts.extra.RECONCILE_REASON"
		private const val NOTIFICATION_CHANNEL_ID = "btandroidts_supervisor"
		private const val NOTIFICATION_ID = 1801

		fun start(context: Context, action: String = ACTION_RECONCILE, reason: String) {
			val intent = Intent(context, PeripheralSupervisorService::class.java)
				.setAction(action)
				.putExtra(EXTRA_REASON, reason)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				ContextCompat.startForegroundService(context, intent)
			} else {
				context.startService(intent)
			}
		}
	}
}
