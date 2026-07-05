package com.cbkii.btandroidts.presentation.feature_opp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbkii.btandroidts.domain.peripheral.FileTransferController
import com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OppHistoryState(
    val history: List<OppTransferHistoryItem> = emptyList()
)

class OppHistoryViewModel(
    private val transferController: FileTransferController
) : ViewModel() {

    val state: StateFlow<OppHistoryState> = transferController.history
        .map { OppHistoryState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OppHistoryState())

    fun retry(id: String) { }

    fun send(context: android.content.Context, request: com.cbkii.btandroidts.domain.peripheral.OppShareRequest): Result<com.cbkii.btandroidts.domain.peripheral.OppTransferHistoryItem> {
        return transferController.delegateToStockOpp(context, request, null).onFailure { err ->
            // Optionally update state or emit failure event here
            // The Result is passed back to caller
            err.printStackTrace()
        }
    }

    fun cancel(id: String) {
        transferController.cancel(id)
    }
}
