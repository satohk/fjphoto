package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.flow.*


class PhotoGridWithSideBarViewModel : ViewModel() {
    private val _sideBarType = MutableStateFlow(SideBarType.TOP)
    val sideBarType: StateFlow<SideBarType> get() = _sideBarType
    private var _gridContents:MutableStateFlow<GridContents> = MutableStateFlow(GridContents(
        SearchQuery()
    ))
    val gridContents: StateFlow<GridContents> get() = _gridContents
    private var _sideBarFocused = MutableStateFlow(true)
    val sideBarFocused: StateFlow<Boolean> get() = _sideBarFocused

    fun subscribeSideBarAction(action: SideBarAction){
        when (action.actionType) {
            SideBarActionType.CHANGE_SIDEBAR -> {
                _sideBarType.value = action.sideBarType!!
            }
            SideBarActionType.BACK -> {
                // TBD back
            }
            SideBarActionType.CHANGE_GRID -> {
                _gridContents.value = action.gridContents!!
            }
            SideBarActionType.ENTER_GRID -> {
                //_gridContents.emit(it.gridContents!!)
                _sideBarFocused.value = false
            }
        }
    }

    fun onBackFromGrid(){
        _sideBarFocused.value = true
    }
}
