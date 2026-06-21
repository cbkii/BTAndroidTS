package com.cbkii.btandroidts.domain.peripheral

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

data class OppShareRequest(
	val items: List<OppShareItem>,
	val mimeType: String?,
) {
	init {
		require(items.isNotEmpty()) { "OPP share request must contain at least one item" }
	}
}

data class OppShareItem(
	val uri: Uri? = null,
	val text: String? = null,
	val displayName: String? = null,
	val mimeType: String? = null,
) {
	init {
		require(uri != null || !text.isNullOrBlank()) { "OPP item must contain a stream URI or text" }
	}
}

interface FileTransferController {
	val history: StateFlow<List<OppTransferHistoryItem>>

	fun delegateToStockOpp(request: OppShareRequest, destination: BluetoothAddress? = null): Result<OppTransferHistoryItem>
	fun cancel(id: String): Result<Unit>
	fun retry(id: String): Result<OppTransferHistoryItem>
}

interface OutgoingTransferStore {
	val history: StateFlow<List<OppTransferHistoryItem>>

	fun put(record: OutgoingTransferRecord)
	fun updateState(id: String, state: OppTransferState, summary: String)
	fun get(id: String): OutgoingTransferRecord?
}

data class OutgoingTransferRecord(
	val request: OppShareRequest,
	val historyItem: OppTransferHistoryItem,
)

enum class OppTransferState {
	QUEUED,
	DELEGATED_TO_STOCK_OPP,
	RUNNING,
	CANCELLED,
	FAILED,
	COMPLETED,
	REQUIRES_DEVICE_VALIDATION,
}

data class OppTransferHistoryItem(
	val id: String,
	val destination: BluetoothAddress?,
	val itemCount: Int,
	val state: OppTransferState,
	val createdAtMillis: Long,
	val summary: String,
)
