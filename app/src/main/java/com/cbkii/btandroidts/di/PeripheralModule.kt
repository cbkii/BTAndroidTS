package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.presentation.navigation.screens.PeripheralManagerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val peripheralModule = module {
    viewModel { PeripheralManagerViewModel(get()) }
}
