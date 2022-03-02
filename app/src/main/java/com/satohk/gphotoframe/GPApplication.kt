package com.satohk.gphotoframe

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import com.satohk.gphotoframe.model.AccountState
import com.satohk.gphotoframe.viewmodel.ChooseAccountViewModel


class GPApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupKoin()
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@GPApplication)
            modules(modules)
        }
    }

    private val modules: Module = module {
        single { AccountState() }
        single { ChooseAccountViewModel() }
    }
}