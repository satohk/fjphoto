package com.satohk.gphotoframe

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.repository.localrepository.PhotoMetadataStore
import com.satohk.gphotoframe.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel


class GPApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupKoin()
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@GPApplication)
            modules(modules)
            initSingletonObjects()
        }
    }

    private fun initSingletonObjects(){
        PhotoMetadataStore.filesDir = applicationContext.filesDir.toString()
    }

    private val modules: Module = module {
        single { AccountState() }
        viewModel { ChooseAccountViewModel( get() ) }
        viewModel { PhotoGridWithSideBarViewModel() }
        viewModel { PhotoGridViewModel( get() ) }
        viewModel { SettingBarViewModel( get() ) }
        viewModel { SearchBarViewModel( get() ) }
        viewModel { MenuBarViewModel( get()) }
        viewModel { PhotoViewModel( get()) }
    }
}