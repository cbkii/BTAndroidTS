package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardScanController
import com.cbkii.btandroidts.domain.phone_keyboard.PhoneKeyboardScanControllerImpl
import com.cbkii.btandroidts.presentation.navigation.screens.phone_keyboard.PhoneKeyboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val phoneKeyboardModule = module {
    single<PhoneKeyboardScanController> {
        PhoneKeyboardScanControllerImpl(
            inventoryRepository = get(),
            hidHostController = get(),
            inputDeviceRepository = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }

    viewModel {
        PhoneKeyboardViewModel(
            scanController = get(),
            bondController = get(),
            hidHostController = get(),
            inputDeviceRepository = get()
        )
    }
}
