package com.satohk.fjphoto.repository.localrepository

import com.satohk.fjphoto.repository.data.SearchQuery
import com.satohk.fjphoto.repository.data.Setting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingRepository(private val db: AppDatabase) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _setting = MutableStateFlow(Setting())
    val setting: StateFlow<Setting> get() = _setting
    private val _id = "1"

    suspend fun set(
        slideShowInterval:Int?=null,
        slideShowOrderIndex:Int?=null,
        slideShowMute:Boolean?=null,
        slideShowCutPlay:Boolean?=null,
        numPhotoGridColumns:Int?=null,
        screensaverSearchQuery:SearchQuery?=null
    ){
        val newSetting = Setting(
            slideShowInterval ?: setting.value.slideShowInterval,
            slideShowOrderIndex ?: setting.value.slideShowOrder,
            slideShowMute ?: setting.value.slideShowMute,
            slideShowCutPlay ?: setting.value.slideShowCutPlay,
            numPhotoGridColumns ?: setting.value.numPhotoGridColumns,
            screensaverSearchQuery ?: setting.value.screensaverSearchQuery)
        save(newSetting)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun load(){
        val settings = withContext(ioDispatcher) {
            db.settingDao().findById(_id)
        }

        if(settings.isNotEmpty()){
            val settingEntity = settings[0]
            val jsonDec = Json { ignoreUnknownKeys = true }
            val searchQuery = jsonDec.decodeFromString<SearchQuery>(settingEntity.screensaverSearchQuery)
            _setting.value = Setting(
                settingEntity.slideShowInterval,
                settingEntity.slideShowOrder,
                settingEntity.slideShowMute,
                settingEntity.slideShowCutPlay,
                settingEntity.numPhotoGridColumns,
                searchQuery)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun save(newSetting: Setting){
        val format = Json { encodeDefaults = false }
        val screensaverSearchQueryStr = format.encodeToString(newSetting.screensaverSearchQuery)
        val settingEntity = SettingEntity(
            _id,
            newSetting.slideShowInterval,
            newSetting.slideShowOrder,
            newSetting.slideShowMute,
            newSetting.slideShowCutPlay,
            newSetting.numPhotoGridColumns,
            screensaverSearchQueryStr
        )
        withContext(ioDispatcher) {
            db.settingDao().save(settingEntity)
        }
        _setting.value = newSetting
    }
}