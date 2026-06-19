package com.cbkii.btandroidts.domain.peripheral

import android.net.Uri

data class OppShareRequest(
	val items: List<OppShareItem>,
	val mimeType: String?,
) {
	init {
		require(items.isNotEmpty()) { "OPP share request must contain at least one item" }
	}
}

data class OppShareItem(
	val uri: Uri,
	val displayName: String? = null,
	val mimeType: String? = null,
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
