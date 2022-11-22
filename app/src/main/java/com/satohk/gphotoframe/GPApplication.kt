package com.satohk.gphotoframe

import android.app.Application
import android.util.Log
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.repository.localrepository.AppDatabase
import com.satohk.gphotoframe.repository.localrepository.SettingRepository
import com.satohk.gphotoframe.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel


class GPApplication : Application() {

    override fun onCreate() {
        Log.d("GPApplication", "onCreate")
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
        single { AccountState(applicationContext) }
        single { Room.databaseBuilder(applicationContext, AppDatabase::class.java, "gphotoframe").build() }
        single { SettingRepository( get() ) }
        viewModel { PhotoGridWithSideBarViewModel() }
        viewModel { PhotoGridViewModel( get() ) }
        viewModel { SettingBarViewModel( get() ) }
        viewModel { SearchBarViewModel( get() ) }
        viewModel { MenuBarViewModel( get()) }
        viewModel { PhotoViewModel( get()) }
    }
}