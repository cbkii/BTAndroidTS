package com.cbkii.btandroidts.data.opp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.FileTransferController
import com.cbkii.btandroidts.domain.peripheral.OppShareRequest
import com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem
import com.cbkii.btandroidts.domain.peripheral.OppTransferState
import com.cbkii.btandroidts.domain.peripheral.OutgoingTransferRecord
import com.cbkii.btandroidts.domain.peripheral.OutgoingTransferStore
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AndroidFileTransferController(
	private val store: OutgoingTransferStore,
) : FileTransferController {

	override val history: StateFlow<List<OppTransferHistoryItem>> = store.history

	override fun delegateToStockOpp(
		launchContext: Context,
		request: OppShareRequest,
		destination: BluetoothAddress?,
	): Result<OppTransferHistoryItem> {
		val id = UUID.randomUUID().toString()
		val initial = OppTransferHistoryItem(
			id = id,
			destination = destination,
			itemCount = request.items.size,
			state = OppTransferState.QUEUED,
			createdAtMillis = System.currentTimeMillis(),
			summary = "Queued for stock Android OPP delegation",
		)
		store.put(OutgoingTransferRecord(request, initial))
		val intent = request.toStockOppIntent()
		if (launchContext !is Activity) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		if (intent.resolveActivity(launchContext.packageManager) == null) {
			val message = "No stock OPP activity resolved"
			store.updateState(id, OppTransferState.FAILED, message)
			return Result.failure(IllegalStateException(message))
		}
		return runCatching {
			launchContext.startActivity(intent)
			store.updateState(
				id = id,
				state = OppTransferState.DELEGATED_TO_STOCK_OPP,
				summary = "Delegated to stock Android Bluetooth OPP; completion requires TS18 validation"
			)
			store.get(id)?.historyItem ?: initial
		}.onFailure { error ->
			store.updateState(id, OppTransferState.FAILED, error.message ?: "OPP delegation failed")
		}
	}

	override fun cancel(id: String): Result<Unit> {
		store.updateState(id, OppTransferState.CANCELLED, "Local transfer entry cancelled; stock OPP cancellation is external")
		return Result.success(Unit)
	}

	override fun retry(launchContext: Context, id: String): Result<OppTransferHistoryItem> {
		val record = store.get(id) ?: return Result.failure(IllegalArgumentException("Unknown transfer id: $id"))
		return delegateToStockOpp(launchContext, record.request, record.historyItem.destination)
	}

	private fun OppShareRequest.toStockOppIntent(): Intent {
		val streamUris = items.mapNotNull { it.uri }
		val text = items.firstNotNullOfOrNull { it.text }
		val type = mimeType ?: items.firstNotNullOfOrNull { it.mimeType } ?: "*/*"
		return when {
			streamUris.size > 1 -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
				setPackage(STOCK_BLUETOOTH_PACKAGE)
				this.type = type
				putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(streamUris))
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
			streamUris.size == 1 -> Intent(Intent.ACTION_SEND).apply {
				setPackage(STOCK_BLUETOOTH_PACKAGE)
				this.type = type
				putExtra(Intent.EXTRA_STREAM, streamUris.single())
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
			else -> Intent(Intent.ACTION_SEND).apply {
				setPackage(STOCK_BLUETOOTH_PACKAGE)
				this.type = type.takeIf { it != "*/*" } ?: "text/plain"
				putExtra(Intent.EXTRA_TEXT, text)
			}
		}
	}

	private companion object {
		const val STOCK_BLUETOOTH_PACKAGE = "com.android.bluetooth"
	}
}
