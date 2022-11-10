package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.AccountState
import kotlinx.coroutines.flow.*

class SettingBarViewModel(
    _accountState: AccountState
    ) : SideBarActionPublisherViewModel() {

    val slideshowIntervalIndex: MutableStateFlow<Int> = MutableStateFlow(2)
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

    init{
        slideshowIntervalIndex.onEach {

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
}
