package com.cbkii.btandroidts.data.opp

import com.cbkii.btandroidts.domain.peripheral.OppShareItem
import com.cbkii.btandroidts.domain.peripheral.OppShareRequest
import com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem
import com.cbkii.btandroidts.domain.peripheral.OppTransferState
import com.cbkii.btandroidts.domain.peripheral.OutgoingTransferRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InMemoryOutgoingTransferStoreTest {

	@Test
	fun storesHistoryAndUpdatesTransferState() {
		val store = InMemoryOutgoingTransferStore()
		val item = OppTransferHistoryItem(
			id = "transfer-1",
			destination = null,
			itemCount = 1,
			state = OppTransferState.QUEUED,
			createdAtMillis = 10L,
			summary = "queued",
		)

		store.put(
			OutgoingTransferRecord(
				request = OppShareRequest(
					items = listOf(OppShareItem(text = "hello", mimeType = "text/plain")),
					mimeType = "text/plain",
				),
				historyItem = item,
			)
		)
		store.updateState("transfer-1", OppTransferState.CANCELLED, "cancelled")

		val updated = store.history.value.single()
		assertEquals(OppTransferState.CANCELLED, updated.state)
		assertEquals("cancelled", updated.summary)
		assertNotNull(store.get("transfer-1"))
	}
}
