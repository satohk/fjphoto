package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class PhotoGridWithSidebarViewModel : ViewModel() {
    private val _sideBarType = MutableStateFlow(SideBarMenuBarType(SideBarType.MENUBAR, MenuBarType.TOP))
    val sideBarType: StateFlow<SideBarMenuBarType> get() = _sideBarType
    private var _searchQuery:MutableStateFlow<SearchQuery?> = MutableStateFlow(SearchQuery())
    val searchQuery: StateFlow<SearchQuery?> get() = _searchQuery
    private var _menuBarFocused = MutableStateFlow(true)
    val menuBarFocused: StateFlow<Boolean> get() = _menuBarFocused

    fun onSelectMenuItem(menuBarItem: MenuBarItem){
        viewModelScope.launch {
            when (menuBarItem.itemType) {
                MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST -> {
                    _sideBarType.emit(SideBarMenuBarType(SideBarType.MENUBAR, MenuBarType.ALBUM_LIST))
                }
                MenuBarItem.MenuBarItemType.SEARCH -> {
                    _sideBarType.emit(SideBarMenuBarType(SideBarType.SEARCH, MenuBarType.NONE))
                }
                MenuBarItem.MenuBarItemType.SETTING -> {
                    _sideBarType.emit(SideBarMenuBarType(SideBarType.SETTING, MenuBarType.NONE))
                }
                else -> {
                    // menubarのアイテムをセレクトしたときはグリッドにフォーカスを移す
                    _menuBarFocused.emit(false)
                }
            }
        }
    }

    fun onFocusMenuItem(menuBarItem: MenuBarItem){
        viewModelScope.launch {
            if(menuBarItem.searchQuery != null){
                _searchQuery.emit(menuBarItem.searchQuery)
            }
        }
    }

    fun onBackFromGrid(){
        viewModelScope.launch {
            _menuBarFocused.emit(true)
        }
    }
}
