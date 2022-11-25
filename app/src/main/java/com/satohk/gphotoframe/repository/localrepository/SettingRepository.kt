package com.satohk.gphotoframe.repository.localrepository

import com.satohk.gphotoframe.repository.data.SearchQuery
import com.satohk.gphotoframe.repository.data.Setting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingRepository(private val db: AppDatabase) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _setting = MutableStateFlow(Setting())
    val setting: StateFlow<Setting> get() = _setting
    private var _userName: String = ""

    suspend fun setSlideShowInterval(value:Int){
        val newSetting = Setting(value, setting.value.numPhotoGridColumns, setting.value.screensaverSearchQuery)
        save(newSetting)
    }
    suspend fun setNumPhotoGridColumns(value:Int){
        val newSetting = Setting(setting.value.slideShowInterval, value, setting.value.screensaverSearchQuery)
        save(newSetting)
    }
    suspend fun setScreensaverSearchQuery(value:SearchQuery){
        val newSetting = Setting(setting.value.slideShowInterval, setting.value.numPhotoGridColumns, value)
        save(newSetting)
    }

    suspend fun load(userName: String){
        val settings = withContext(ioDispatcher) {
            db.settingDao().findByUserName(userName)
        }

        if(settings.isNotEmpty()){
            val settingEntity = settings[0]
            val jsonDec = Json { ignoreUnknownKeys = true }
            val searchQuery = jsonDec.decodeFromString<SearchQuery>(settingEntity.screensaverSearchQuery)
            _setting.value = Setting(
                settingEntity.slideShowInterval,
                settingEntity.numPhotoGridColumns,
                searchQuery)
        }

        _userName = userName
    }

    private suspend fun save(newSetting: Setting){
        val format = Json { encodeDefaults = false }
        val screensaverSearchQueryStr = format.encodeToString(newSetting.screensaverSearchQuery)
        val settingEntity = SettingEntity(
            _userName,
            newSetting.slideShowInterval,
            newSetting.numPhotoGridColumns,
            screensaverSearchQueryStr
        )
        withContext(ioDispatcher) {
            db.settingDao().save(settingEntity)
        }
        _setting.value = newSetting
    }
}