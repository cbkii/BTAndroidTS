package com.cbkii.btandroidts.data.bluetooth_le

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal enum class GattCallbackType {
	SERVICES_DISCOVERED,
	RSSI_READ,
	MTU_CHANGED,
	CHARACTERISTIC_READ,
	CHARACTERISTIC_WRITE,
	DESCRIPTOR_READ,
	DESCRIPTOR_WRITE,
}

internal data class GattAttributeKey(
	val serviceUuid: UUID? = null,
	val characteristicUuid: UUID? = null,
	val descriptorUuid: UUID? = null,
	val instanceId: Int? = null,
)

internal data class ExpectedGattCallback(
	val type: GattCallbackType,
	val key: GattAttributeKey? = null,
) {
	fun matches(event: GattCallbackEvent): Boolean {
		return event.type == type && (key == null || key == event.key)
	}
}

internal data class GattCallbackEvent(
	val sessionToken: Any,
	val type: GattCallbackType,
	val key: GattAttributeKey? = null,
	val successful: Boolean,
	val detail: String? = null,
)

internal sealed class GattOperationException(message: String) : IllegalStateException(message) {
	class NoActiveSession : GattOperationException("No active Bluetooth GATT session")
	class QueueClosed : GattOperationException("Bluetooth GATT operation queue is closed")
	class SessionReplaced : GattOperationException("Bluetooth GATT session was replaced")
	class SessionDisconnected : GattOperationException("Bluetooth GATT session disconnected")
	class StartRejected(operation: String) :
		GattOperationException("Bluetooth GATT operation was not accepted: $operation")
	class CallbackFailed(operation: String, detail: String?) : GattOperationException(
		buildString {
			append("Bluetooth GATT callback failed: ")
			append(operation)
			if (!detail.isNullOrBlank()) {
				append(" (")
				append(detail)
				append(')')
			}
		}
	)
	class TimedOut(operation: String, timeoutMs: Long) :
		GattOperationException("Bluetooth GATT operation timed out after ${timeoutMs}ms: $operation")
}

/**
 * Serializes callback-driven Android BluetoothGatt operations for one active GATT session.
 *
 * A request keeps the queue until its matching callback, an immediate native start rejection,
 * a timeout, a disconnect, or queue closure. Callbacks from an earlier GATT instance are ignored.
 */
internal class GattOperationQueue(
	private val scope: CoroutineScope,
	private val defaultTimeoutMs: Long = DEFAULT_OPERATION_TIMEOUT_MS,
	capacity: Int = DEFAULT_QUEUE_CAPACITY,
) {
	private data class Request(
		val id: Long,
		val name: String,
		val sessionToken: Any,
		val expectedCallback: ExpectedGattCallback,
		val timeoutMs: Long,
		val start: () -> Boolean,
		val completion: CompletableDeferred<Result<Unit>> = CompletableDeferred(),
	)

	private val closed = AtomicBoolean(false)
	private val requestIds = AtomicLong(0)
	private val requests = Channel<Request>(capacity = capacity)

	@Volatile
	private var sessionToken: Any? = null

	@Volatile
	private var activeRequest: Request? = null

	private val worker: Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
		for (request in requests) {
			process(request)
		}
	}

	fun attachSession(token: Any) {
		if (closed.get()) return

		val previous = sessionToken
		if (previous != null && previous !== token) {
			failActiveAndPending(GattOperationException.SessionReplaced())
		}
		sessionToken = token
	}

	suspend fun execute(
		name: String,
		expectedCallback: ExpectedGattCallback,
		timeoutMs: Long = defaultTimeoutMs,
		start: () -> Boolean,
	): Result<Unit> {
		if (closed.get()) return Result.failure(GattOperationException.QueueClosed())
		val token = sessionToken ?: return Result.failure(GattOperationException.NoActiveSession())

		val request = Request(
			id = requestIds.incrementAndGet(),
			name = name,
			sessionToken = token,
			expectedCallback = expectedCallback,
			timeoutMs = timeoutMs,
			start = start,
		)

		return try {
			requests.send(request)
			request.completion.await()
		} catch (error: Throwable) {
			if (error is kotlinx.coroutines.CancellationException) throw error
			Result.failure(error)
		}
	}

	fun onCallback(event: GattCallbackEvent): Boolean {
		if (closed.get()) return false
		if (event.sessionToken !== sessionToken) return false

		val request = activeRequest ?: return false
		if (request.sessionToken !== event.sessionToken) return false
		if (!request.expectedCallback.matches(event)) return false

		val result = if (event.successful) {
			Result.success(Unit)
		} else {
			Result.failure(GattOperationException.CallbackFailed(request.name, event.detail))
		}
		return request.completion.complete(result)
	}

	fun failActiveAndPending(cause: Throwable) {
		activeRequest?.completion?.complete(Result.failure(cause))
		while (true) {
			val pending = requests.tryReceive().getOrNull() ?: break
			pending.completion.complete(Result.failure(cause))
		}
	}

	fun close(cause: Throwable = GattOperationException.QueueClosed()) {
		if (!closed.compareAndSet(false, true)) return

		sessionToken = null
		requests.close(cause)
		failActiveAndPending(cause)
		worker.cancel(cause.message ?: "GATT queue closed", cause)
	}

	private suspend fun process(request: Request) {
		if (closed.get()) {
			request.completion.complete(Result.failure(GattOperationException.QueueClosed()))
			return
		}
		if (request.sessionToken !== sessionToken) {
			request.completion.complete(Result.failure(GattOperationException.SessionReplaced()))
			return
		}

		activeRequest = request
		try {
			val started = try {
				request.start()
			} catch (error: Throwable) {
				request.completion.complete(Result.failure(error))
				false
			}

			if (!started && !request.completion.isCompleted) {
				request.completion.complete(
					Result.failure(GattOperationException.StartRejected(request.name))
				)
			}

			if (started) {
				val completed = withTimeoutOrNull(request.timeoutMs) {
					request.completion.await()
					true
				} ?: false

				if (!completed) {
					request.completion.complete(
						Result.failure(GattOperationException.TimedOut(request.name, request.timeoutMs))
					)
				}
			}
		} finally {
			if (activeRequest === request) activeRequest = null
		}
	}

	private companion object {
		const val DEFAULT_OPERATION_TIMEOUT_MS = 10_000L
		const val DEFAULT_QUEUE_CAPACITY = 32
	}
}
