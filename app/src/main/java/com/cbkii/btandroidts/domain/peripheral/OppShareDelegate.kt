package com.cbkii.btandroidts.domain.peripheral
import android.net.Uri
interface OppShareDelegate {
    fun sendFile(uri: Uri): Result<OppTransferHistoryItem>
}
