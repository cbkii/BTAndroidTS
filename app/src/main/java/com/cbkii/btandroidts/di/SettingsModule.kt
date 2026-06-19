package com.cbkii.btandroidts.di

import com.cbkii.btandroidts.data.datastore.BLESettingsDatastoreImpl
import com.cbkii.btandroidts.data.datastore.BTSettingsDatastoreImpl
import com.cbkii.btandroidts.domain.settings.repository.BLESettingsDataStore
import com.cbkii.btandroidts.domain.settings.repository.BTSettingsDataSore
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsModule = module {
	// low energy datastore
	factoryOf(::BLESettingsDatastoreImpl).bind<BLESettingsDataStore>()
	// classic settings datastore
	factoryOf(::BTSettingsDatastoreImpl).bind<BTSettingsDataSore>()

}