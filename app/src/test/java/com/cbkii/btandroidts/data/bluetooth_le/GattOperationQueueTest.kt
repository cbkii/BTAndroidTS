package com.cbkii.btandroidts.data.bluetooth_le

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GattOperationQueueTest {

	@Test
	fun operationsWithinOneSessionWaitForMatchingCallbacks() = runTest {
		val token = Any()
		val starts = mutableListOf<String>()
		val queue = GattOperationQueue(this, defaultTimeoutMs = 1_000)
		queue.attachSession(token)

		val first = async {
			queue.execute(
				name = "first",
				expectedCallback = ExpectedGattCallback(GattCallbackType.RSSI_READ),
				start = { starts += "first"; true },
			)
		}
		runCurrent()

		val second = async {
			queue.execute(
				name = "second",
				expectedCallback = ExpectedGattCallback(GattCallbackType.MTU_CHANGED),
				start = { starts += "second"; true },
			)
		}
		runCurrent()
		assertEquals(listOf("first"), starts)

		assertTrue(queue.onCallback(success(token, GattCallbackType.RSSI_READ)))
		runCurrent()
		assertEquals(listOf("first", "second"), starts)
		assertTrue(first.await().isSuccess)

		assertTrue(queue.onCallback(success(token, GattCallbackType.MTU_CHANGED)))
		runCurrent()
		assertTrue(second.await().isSuccess)
	}

	@Test
	fun separateSessionQueuesCanProgressConcurrently() = runTest {
		val tokenA = Any()
		val tokenB = Any()
		val starts = mutableListOf<String>()
		val queueA = GattOperationQueue(this)
		val queueB = GattOperationQueue(this)
		queueA.attachSession(tokenA)
		queueB.attachSession(tokenB)

		val resultA = async {
			queueA.execute("A", ExpectedGattCallback(GattCallbackType.RSSI_READ)) {
				starts += "A"
				true
			}
		}
		val resultB = async {
			queueB.execute("B", ExpectedGattCallback(GattCallbackType.RSSI_READ)) {
				starts += "B"
				true
			}
		}
		runCurrent()
		assertEquals(setOf("A", "B"), starts.toSet())

		queueA.onCallback(success(tokenA, GattCallbackType.RSSI_READ))
		queueB.onCallback(success(tokenB, GattCallbackType.RSSI_READ))
		runCurrent()
		assertTrue(resultA.await().isSuccess)
		assertTrue(resultB.await().isSuccess)
	}

	@Test
	fun immediateStartRejectionReleasesTheNextOperation() = runTest {
		val token = Any()
		val starts = mutableListOf<String>()
		val queue = GattOperationQueue(this)
		queue.attachSession(token)

		val rejected = async {
			queue.execute("rejected", ExpectedGattCallback(GattCallbackType.RSSI_READ)) {
				starts += "rejected"
				false
			}
		}
		val next = async {
			queue.execute("next", ExpectedGattCallback(GattCallbackType.MTU_CHANGED)) {
				starts += "next"
				true
			}
		}
		runCurrent()

		assertTrue(rejected.await().exceptionOrNull() is GattOperationException.StartRejected)
		assertEquals(listOf("rejected", "next"), starts)
		queue.onCallback(success(token, GattCallbackType.MTU_CHANGED))
		runCurrent()
		assertTrue(next.await().isSuccess)
	}

	@Test
	fun callbackFailureReleasesTheNextOperation() = runTest {
		val token = Any()
		val starts = mutableListOf<String>()
		val queue = GattOperationQueue(this)
		queue.attachSession(token)

		val failed = async {
			queue.execute("failed", ExpectedGattCallback(GattCallbackType.RSSI_READ)) {
				starts += "failed"
				true
			}
		}
		val next = async {
			queue.execute("next", ExpectedGattCallback(GattCallbackType.MTU_CHANGED)) {
				starts += "next"
				true
			}
		}
		runCurrent()

		queue.onCallback(
			GattCallbackEvent(
				sessionToken = token,
				type = GattCallbackType.RSSI_READ,
				successful = false,
				detail = "status=133",
			)
		)
		runCurrent()
		assertTrue(failed.await().exceptionOrNull() is GattOperationException.CallbackFailed)
		assertEquals(listOf("failed", "next"), starts)

		queue.onCallback(success(token, GattCallbackType.MTU_CHANGED))
		runCurrent()
		assertTrue(next.await().isSuccess)
	}

	@Test
	fun missingCallbackTimesOutAndQueueContinues() = runTest {
		val token = Any()
		val starts = mutableListOf<String>()
		val queue = GattOperationQueue(this, defaultTimeoutMs = 100)
		queue.attachSession(token)

		val timedOut = async {
			queue.execute("timeout", ExpectedGattCallback(GattCallbackType.RSSI_READ)) {
				starts += "timeout"
				true
			}
		}
		val next = async {
			queue.execute("next", ExpectedGattCallback(GattCallbackType.MTU_CHANGED)) {
				starts += "next"
				true
			}
		}
		runCurrent()
		advanceTimeBy(100)
		runCurrent()

		assertTrue(timedOut.await().exceptionOrNull() is GattOperationException.TimedOut)
		assertEquals(listOf("timeout", "next"), starts)
		queue.onCallback(success(token, GattCallbackType.MTU_CHANGED))
		runCurrent()
		assertTrue(next.await().isSuccess)
	}

	@Test
	fun staleAndMismatchedCallbacksDoNotCompleteActiveOperation() = runTest {
		val token = Any()
		val staleToken = Any()
		val key = GattAttributeKey(
			serviceUuid = UUID.randomUUID(),
			characteristicUuid = UUID.randomUUID(),
			instanceId = 1,
		)
		val otherKey = key.copy(instanceId = 2)
		val queue = GattOperationQueue(this)
		queue.attachSession(token)

		val result = async {
			queue.execute(
				name = "read",
				expectedCallback = ExpectedGattCallback(GattCallbackType.CHARACTERISTIC_READ, key),
				start = { true },
			)
		}
		runCurrent()

		assertFalse(
			queue.onCallback(
				success(staleToken, GattCallbackType.CHARACTERISTIC_READ, key),
			)
		)
		assertFalse(
			queue.onCallback(
				success(token, GattCallbackType.CHARACTERISTIC_READ, otherKey),
			)
		)
		assertFalse(result.isCompleted)

		assertTrue(queue.onCallback(success(token, GattCallbackType.CHARACTERISTIC_READ, key)))
		runCurrent()
		assertTrue(result.await().isSuccess)
	}

	@Test
	fun disconnectFailsActiveAndPendingRequestsWithoutClosingQueue() = runTest {
		val token = Any()
		val queue = GattOperationQueue(this)
		queue.attachSession(token)

		val active = async {
			queue.execute("active", ExpectedGattCallback(GattCallbackType.RSSI_READ)) { true }
		}
		val pending = async {
			queue.execute("pending", ExpectedGattCallback(GattCallbackType.MTU_CHANGED)) { true }
		}
		runCurrent()

		queue.failActiveAndPending(GattOperationException.SessionDisconnected())
		runCurrent()
		assertTrue(active.await().exceptionOrNull() is GattOperationException.SessionDisconnected)
		assertTrue(pending.await().exceptionOrNull() is GattOperationException.SessionDisconnected)

		val afterReconnect = async {
			queue.execute("after", ExpectedGattCallback(GattCallbackType.RSSI_READ)) { true }
		}
		runCurrent()
		queue.onCallback(success(token, GattCallbackType.RSSI_READ))
		runCurrent()
		assertTrue(afterReconnect.await().isSuccess)
	}

	@Test
	fun replacingSessionRejectsOldWorkAndIgnoresOldCallback() = runTest {
		val oldToken = Any()
		val newToken = Any()
		val queue = GattOperationQueue(this)
		queue.attachSession(oldToken)

		val old = async {
			queue.execute("old", ExpectedGattCallback(GattCallbackType.RSSI_READ)) { true }
		}
		runCurrent()
		queue.attachSession(newToken)
		runCurrent()
		assertTrue(old.await().exceptionOrNull() is GattOperationException.SessionReplaced)
		assertFalse(queue.onCallback(success(oldToken, GattCallbackType.RSSI_READ)))

		val current = async {
			queue.execute("new", ExpectedGattCallback(GattCallbackType.RSSI_READ)) { true }
		}
		runCurrent()
		assertTrue(queue.onCallback(success(newToken, GattCallbackType.RSSI_READ)))
		runCurrent()
		assertTrue(current.await().isSuccess)
	}

	@Test
	fun callerCancellationDoesNotReleaseInFlightGattOperationEarly() = runTest {
		val token = Any()
		val starts = mutableListOf<String>()
		val queue = GattOperationQueue(this)
		queue.attachSession(token)

		val cancelledCaller = launch {
			queue.execute("first", ExpectedGattCallback(GattCallbackType.RSSI_READ)) {
				starts += "first"
				true
			}
		}
		runCurrent()
		cancelledCaller.cancelAndJoin()

		val next = async {
			queue.execute("next", ExpectedGattCallback(GattCallbackType.MTU_CHANGED)) {
				starts += "next"
				true
			}
		}
		runCurrent()
		assertEquals(listOf("first"), starts)

		queue.onCallback(success(token, GattCallbackType.RSSI_READ))
		runCurrent()
		assertEquals(listOf("first", "next"), starts)
		queue.onCallback(success(token, GattCallbackType.MTU_CHANGED))
		runCurrent()
		assertTrue(next.await().isSuccess)
	}

	@Test
	fun closedQueueRejectsNewWork() = runTest {
		val queue = GattOperationQueue(this)
		queue.attachSession(Any())
		queue.close()

		val result = queue.execute(
			name = "closed",
			expectedCallback = ExpectedGattCallback(GattCallbackType.RSSI_READ),
			start = { true },
		)
		assertTrue(result.exceptionOrNull() is GattOperationException.QueueClosed)
	}

	private fun success(
		token: Any,
		type: GattCallbackType,
		key: GattAttributeKey? = null,
	) = GattCallbackEvent(
		sessionToken = token,
		type = type,
		key = key,
		successful = true,
	)
}
