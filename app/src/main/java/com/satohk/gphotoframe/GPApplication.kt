package com.satohk.gphotoframe

import android.app.Application
import android.util.Log
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.domain.InferenceModel
import com.satohk.gphotoframe.domain.InferenceModelLoader
import com.satohk.gphotoframe.domain.VisualInspector
import com.satohk.gphotoframe.repository.localrepository.AppDatabase
import com.satohk.gphotoframe.repository.localrepository.PhotoMetadataLocalRepository
import com.satohk.gphotoframe.repository.localrepository.SettingRepository
import com.satohk.gphotoframe.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.java.KoinJavaComponent


class GPApplication : Application() {

    override fun onCreate() {
        Log.d("GPApplication", "onCreate")
        super.onCreate()
        setupKoin()
    }

    override fun onTerminate() {
        super.onTerminate()

        val inferenceModelLoader: InferenceModelLoader by KoinJavaComponent.inject(InferenceModelLoader::class.java)
        inferenceModelLoader.close()
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
        single { PhotoMetadataLocalRepository( get() ) }
        single { InferenceModelLoader(applicationContext) }
        single { InferenceModel( get() ) }
        single { VisualInspector( get() ) }
        viewModel { PhotoGridWithSideBarViewModel() }
        viewModel { PhotoGridViewModel( get(), get() ) }
        viewModel { SettingBarViewModel( get() ) }
        viewModel { SearchBarViewModel( get(), get() ) }
        viewModel { MenuBarViewModel( get()) }
        viewModel { PhotoViewModel( get()) }
    }
}