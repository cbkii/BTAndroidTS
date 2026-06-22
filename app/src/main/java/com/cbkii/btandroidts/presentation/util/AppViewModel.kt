package com.cbkii.btandroidts.presentation.util

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.SharedFlow

abstract class AppViewModel : ViewModel() {

	abstract val uiEvents: SharedFlow<UiEvents>
}