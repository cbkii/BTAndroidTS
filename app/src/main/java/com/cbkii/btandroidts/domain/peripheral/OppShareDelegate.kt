package com.cbkii.btandroidts.domain.peripheral
import android.net.Uri
interface OppShareDelegate {
    suspend fun sendFile(uri: Uri): Result<OppTransferHistoryItem>
}
