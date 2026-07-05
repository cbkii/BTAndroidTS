package com.cbkii.btandroidts.data.opp
import android.content.Context
import android.net.Uri
import com.cbkii.btandroidts.domain.peripheral.FileTransferController
import com.cbkii.btandroidts.domain.peripheral.OppShareDelegate
import com.cbkii.btandroidts.domain.peripheral.OppShareItem
import com.cbkii.btandroidts.domain.peripheral.OppShareRequest
import com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem

class AndroidOppShareDelegate(
    private val context: Context,
    private val transferController: FileTransferController
) : OppShareDelegate {
    override fun sendFile(uri: Uri): Result<OppTransferHistoryItem> {
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        val request = OppShareRequest(
            items = listOf(OppShareItem(uri = uri, text = null, mimeType = mimeType)),
            mimeType = mimeType
        )
        return transferController.delegateToStockOpp(context, request, null)
    }
}
