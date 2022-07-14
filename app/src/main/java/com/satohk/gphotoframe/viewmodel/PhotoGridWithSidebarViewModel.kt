package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class PhotoGridWithSidebarViewModel : ViewModel() {
    private val _sideBarType = MutableStateFlow(SideBarType.TOP)
    val sidebarType: StateFlow<SideBarType> get() = _sideBarType
    private var _gridContents:MutableStateFlow<GridContents> = MutableStateFlow(GridContents(
        SearchQuery()
    ))
    val gridContents: StateFlow<GridContents> get() = _gridContents
    private var _sideBarFocused = MutableStateFlow(true)
    val sidebarFocused: StateFlow<Boolean> get() = _sideBarFocused

    fun setSidebarType(sideBarType: SideBarType){
        viewModelScope.launch {
            _sideBarType.emit(sideBarType)
        }
    }

    fun onSidebarAction(sideBarAction: SideBarAction){
        viewModelScope.launch {
            when (sideBarAction.actionType) {
                SideBarActionType.CHANGE_SIDEBAR -> {
                    _sideBarType.emit(sideBarAction.sideBarType!!)
                }
                SideBarActionType.BACK -> {
                    // TBD back
                }
                SideBarActionType.CHANGE_GRID -> {
                    _gridContents.emit(sideBarAction.gridContents!!)
                }
                SideBarActionType.ENTER_GRID -> {
                    _gridContents.emit(sideBarAction.gridContents!!)
                    _sideBarFocused.emit(false)
                }
            }
        }
    }

    fun onBackFromGrid(){
        viewModelScope.launch {
            _sideBarFocused.emit(true)
        }
    }
}
