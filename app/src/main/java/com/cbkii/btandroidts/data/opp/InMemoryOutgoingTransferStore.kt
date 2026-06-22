package com.cbkii.btandroidts.data.opp

import com.cbkii.btandroidts.domain.peripheral.OppTransferState
import com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem
import com.cbkii.btandroidts.domain.peripheral.OutgoingTransferRecord
import com.cbkii.btandroidts.domain.peripheral.OutgoingTransferStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryOutgoingTransferStore : OutgoingTransferStore {

	private val lock = Any()
	private val records = linkedMapOf<String, OutgoingTransferRecord>()
	private val _history = MutableStateFlow<List<OppTransferHistoryItem>>(emptyList())
	override val history: StateFlow<List<OppTransferHistoryItem>> = _history.asStateFlow()

	override fun put(record: OutgoingTransferRecord) {
		synchronized(lock) {
			records[record.historyItem.id] = record
			_history.value =
				(listOf(record.historyItem) + _history.value.filterNot { it.id == record.historyItem.id }).take(MAX_HISTORY)
		}
	}

	override fun updateState(id: String, state: OppTransferState, summary: String) {
		synchronized(lock) {
			val record = records[id] ?: return@synchronized
			val updated = record.copy(
				historyItem = record.historyItem.copy(
					state = state,
					summary = summary,
				)
			)
			records[id] = updated
			_history.value = _history.value.map { if (it.id == id) updated.historyItem else it }
		}
	}

	override fun get(id: String): OutgoingTransferRecord? =
		synchronized(lock) { records[id] }

	private companion object {
		const val MAX_HISTORY = 50
	}
}
