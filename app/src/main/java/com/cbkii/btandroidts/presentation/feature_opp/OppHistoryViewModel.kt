package com.cbkii.btandroidts.presentation.feature_opp

import androidx.lifecycle.ViewModel
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.FileTransferController
import com.cbkii.btandroidts.domain.peripheral.OppShareDelegate
import com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OppHistoryState(
    val history: List<OppTransferHistoryItem> = emptyList()
)

class OppHistoryViewModel(
    private val transferController: FileTransferController,
    private val oppShareDelegate: OppShareDelegate
) : ViewModel() {

    val state: StateFlow<OppHistoryState> = transferController.history
        .map { OppHistoryState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OppHistoryState())

    fun retry(id: String) { }

    fun sendFile(uri: Uri): Result<OppTransferHistoryItem> {
        return oppShareDelegate.sendFile(uri)
    }

    fun cancel(id: String) {
        transferController.cancel(id)
    }
}
