package com.satohk.gphotoframe

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.satohk.gphotoframe.domain.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import com.satohk.gphotoframe.repository.localrepository.AppDatabase
import com.satohk.gphotoframe.repository.localrepository.PhotoMetadataLocalRepository
import com.satohk.gphotoframe.repository.localrepository.SettingRepository
import com.satohk.gphotoframe.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.java.KoinJavaComponent


class GPApplication : Application() {

    override fun onCreate() {
        Log.d("GPApplication", "onCreate")
        super.onCreate()
        setupKoin()

        modulesAsyncInit()
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
        single { FilteredPhotoList( get() ) }
        viewModel { MainViewModel( get()) }
        viewModel { PhotoGridWithSideBarViewModel() }
        viewModel { PhotoGridViewModel( get(), get() ) }
        viewModel { SettingBarViewModel( get() ) }
        viewModel { SearchBarViewModel( get(), get() ) }
        viewModel { MenuBarViewModel( get()) }
        viewModel { PhotoViewModel( get()) }
    }

    private fun modulesAsyncInit(){
        val scope = CoroutineScope(Job() + Dispatchers.IO)

        val settingRepository: SettingRepository by KoinJavaComponent.inject(SettingRepository::class.java)
        scope.launch{
            settingRepository.load()
        }
    }
}