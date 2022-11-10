package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import com.satohk.gphotoframe.domain.*
import com.satohk.gphotoframe.repository.entity.SearchQuery
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

    private val _backStack: MutableList<SideBarAction> = mutableListOf()
    val backStackSize  get() = _backStack.size

    fun subscribeSideBarAction(action: SideBarAction, addBackStack: Boolean){
        if(addBackStack){
            when(action.actionType){
                SideBarActionType.CHANGE_SIDEBAR -> {
                    _backStack.add(SideBarAction(SideBarActionType.CHANGE_SIDEBAR, sideBarType.value, null))
                }
                SideBarActionType.ENTER_GRID -> {
                    _backStack.add(SideBarAction(SideBarActionType.LEAVE_GRID, null, null))
                }
            }
        }
        when (action.actionType) {
            SideBarActionType.CHANGE_SIDEBAR -> {
                _sideBarType.value = action.sideBarType!!
            }
            SideBarActionType.BACK -> {
                if(_backStack.size > 0) {
                    val backAction = _backStack.last()
                    _backStack.removeLast()
                    subscribeSideBarAction(backAction, false)
                }
            }
            SideBarActionType.CHANGE_GRID -> {
                _gridContents.value = action.gridContents!!
            }
            SideBarActionType.ENTER_GRID -> {
                //_gridContents.emit(it.gridContents!!)
                _sideBarFocused.value = false
            }
            SideBarActionType.LEAVE_GRID -> {
                //_gridContents.emit(it.gridContents!!)
                _sideBarFocused.value = true
            }
        }
    }

    fun onBackFromGrid(){
        _sideBarFocused.value = true
    }
}
