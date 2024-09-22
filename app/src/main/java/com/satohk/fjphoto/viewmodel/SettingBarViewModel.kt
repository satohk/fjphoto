package com.satohk.fjphoto.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class SettingBarViewModel(
    private val _accountState: AccountState
    ) : SideBarActionPublisherViewModel() {

    val slideshowIntervalIndex: MutableStateFlow<Int> = MutableStateFlow(9)
    private val _slideshowIntervalList: MutableStateFlow<List<String>>
        = MutableStateFlow(listOf(
            "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"
        ))
    val slideshowIntervalList: StateFlow<List<String>> get() = _slideshowIntervalList

    val slideshowOrderIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val slideshowMute: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val slideshowCutPlay: MutableStateFlow<Boolean> = MutableStateFlow(true)

    val columnNumIndex: MutableStateFlow<Int> = MutableStateFlow(2)
    private val _columnNumList: MutableStateFlow<List<String>>
            = MutableStateFlow(listOf(
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
    ))
    val columnNumList: StateFlow<List<String>> get() = _columnNumList

    private val _displayMessageId = MutableSharedFlow<Int>()
    val displayMessageId: SharedFlow<Int?> get() = _displayMessageId

    private val _syncedPhotoLastDate = MutableStateFlow<ZonedDateTime?>(null)
    val syncedPhotoLastDate: StateFlow<ZonedDateTime?> get() = _syncedPhotoLastDate

    init{
        slideshowIntervalIndex.onEach {
            Utils.spinnerIndex2str(it, _slideshowIntervalList.value)?.let { strVal ->
                strVal.toIntOrNull()?.let { intVal ->
                    _accountState.settingRepository.set(slideShowInterval=intVal)
                }
            }
        }.launchIn(viewModelScope)

        slideshowOrderIndex.onEach { _accountState.settingRepository.set(slideShowOrderIndex=it) }.launchIn(viewModelScope)
        slideshowMute.onEach { _accountState.settingRepository.set(slideShowMute=it) }.launchIn(viewModelScope)
        slideshowCutPlay.onEach { _accountState.settingRepository.set(slideShowCutPlay=it) }.launchIn(viewModelScope)

        columnNumIndex.onEach {
            Utils.spinnerIndex2str(it, _columnNumList.value)?.let { strVal ->
                strVal.toIntOrNull()?.let { intVal ->
                    _accountState.settingRepository.set(numPhotoGridColumns = intVal)
                }
            }
        }.launchIn(viewModelScope)

        _accountState.settingRepository.setting.onEach{
            slideshowIntervalIndex.value = _slideshowIntervalList.value.indexOf(it.slideShowInterval.toString())
            slideshowOrderIndex.value = it.slideShowOrder
            slideshowMute.value = it.slideShowMute
            slideshowCutPlay.value = it.slideShowCutPlay
            columnNumIndex.value = _columnNumList.value.indexOf(it.numPhotoGridColumns.toString())
        }.launchIn(viewModelScope)
    }

    fun updateInfo(){
        viewModelScope.launch{
            Log.d("SettingBarViewModel", "updateInfo")
            if(_accountState?.activeAccount?.value?.accountId != null) {
                _syncedPhotoLastDate.value =
                    _accountState.photoMetadataRemoteCacheRepository.getLast(_accountState!!.activeAccount!!.value!!.accountId)?.timestamp
            }
        }
    }
}
