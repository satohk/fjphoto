package com.satohk.fjphoto

import android.app.Application
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.satohk.fjphoto.domain.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

import com.satohk.fjphoto.repository.localrepository.AppDatabase
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataLocalRepository
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataRemoteCacheRepository
import com.satohk.fjphoto.repository.localrepository.SettingRepository
import com.satohk.fjphoto.view.VideoCache
import com.satohk.fjphoto.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.java.KoinJavaComponent


@UnstableApi
class FJPhotoApplication : Application() {

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
            androidContext(this@FJPhotoApplication)
            modules(modules)
        }
    }

    private val modules: Module = module {
        single { AccountState(applicationContext) }
        single { Room.databaseBuilder(applicationContext, AppDatabase::class.java, "fjphoto").build() }
        single { SettingRepository( get() ) }
        single { PhotoMetadataLocalRepository( get() ) }
        single { PhotoMetadataRemoteCacheRepository( get() ) }
        single { InferenceModelLoader(applicationContext) }
        single { InferenceModel( get() ) }
        single { VisualInspector( get() ) }
        single { FilteredPhotoList( get() ) }
        single { VideoCache(applicationContext) }
        viewModel { MainViewModel( get()) }
        viewModel { ScreenSaverViewModel( get()) }
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