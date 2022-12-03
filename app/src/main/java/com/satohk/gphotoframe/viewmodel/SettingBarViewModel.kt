package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.domain.FilteredPhotoList
import com.satohk.gphotoframe.domain.VisualInspector
import com.satohk.gphotoframe.repository.data.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent

class SettingBarViewModel(
    private val _accountState: AccountState
    ) : SideBarActionPublisherViewModel() {

    private val _visualInspector: VisualInspector by KoinJavaComponent.inject(VisualInspector::class.java)

    val slideshowIntervalIndex: MutableStateFlow<Int> = MutableStateFlow(9)
    private val _slideshowIntervalList: MutableStateFlow<List<String>>
        = MutableStateFlow(listOf(
            "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"
        ))
    val slideshowIntervalList: StateFlow<List<String>> get() = _slideshowIntervalList

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

    private val _inTraining = MutableStateFlow<Boolean>(false)
    val inTraining: StateFlow<Boolean> get() = _inTraining


    init{
        slideshowIntervalIndex.onEach {
            Utils.spinnerIndex2str(it, _slideshowIntervalList.value)?.let { strVal ->
                strVal.toIntOrNull()?.let { intVal ->
                    _accountState.settingRepository.setSlideShowInterval(intVal)
                }
            }
        }.launchIn(viewModelScope)

        columnNumIndex.onEach {
            Utils.spinnerIndex2str(it, _columnNumList.value)?.let { strVal ->
                strVal.toIntOrNull()?.let { intVal ->
                    _accountState.settingRepository.setNumPhotoGridColumns(intVal)
                }
            }
        }.launchIn(viewModelScope)

        _accountState.settingRepository.setting.onEach{
            slideshowIntervalIndex.value = _slideshowIntervalList.value.indexOf(it.slideShowInterval.toString())
            columnNumIndex.value = _columnNumList.value.indexOf(it.numPhotoGridColumns.toString())
        }.launchIn(viewModelScope)
    }

    fun enterToGrid() {
        val action = SideBarAction(
            SideBarActionType.ENTER_GRID,
            gridContents = null
        )
        publishAction(action)
    }

    fun goBack(){
        val action = SideBarAction(
            SideBarActionType.BACK,
            gridContents = null
        )
        publishAction(action)
    }

    fun doTrainAIModel(){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _displayMessageId.emit(R.string.msg_started_learning)
                _inTraining.value = true
                val photoList = FilteredPhotoList(_accountState.photoRepository.value!!, SearchQuery())
                photoList.loadNext(100)
                val bmpList = mutableListOf<Bitmap>()
                for(i in 0 until photoList.size){
                    val bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                        photoList[i].metadataRemote,
                        _visualInspector.inputImageSize.width,
                        _visualInspector.inputImageSize.width,
                        true
                    )
                    bmp?.let{ bmpList.add(it) }
                }
                _visualInspector.calcWholeFeatures(bmpList)
                _displayMessageId.emit(R.string.msg_finished_learning)
                _inTraining.value = false
            }
        }
    }
}
