package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


class MenuBarViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)
    private val _allItemList = mutableMapOf<MenuBarType, List<MenuBarItem>>()
    private val _selectedItemIndex = mutableMapOf<MenuBarType, Int>()
    private var _selectedMenuBarType: MenuBarType = MenuBarType.TOP
    var selectedMenuBarType: MenuBarType
        get() = _selectedMenuBarType
        set(value){
            _selectedMenuBarType = value
            if(_allItemList[_selectedMenuBarType] != null) {
                _itemList.value = _allItemList[_selectedMenuBarType]!!
            }
        }

    private val _itemList = MutableStateFlow(listOf<MenuBarItem>())
    val itemList: StateFlow<List<MenuBarItem>> get() = _itemList
    var selectedItemIndex: Int
        get() = _selectedItemIndex[_selectedMenuBarType]!!
        set(value) { _selectedItemIndex[_selectedMenuBarType] = value}
    val selectedItem: MenuBarItem
        get() = itemList.value[selectedItemIndex]

    init{
        _allItemList[MenuBarType.TOP] = listOf(
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_ALL, searchQuery=SearchQuery(mediaType=SearchQuery.MediaType.ALL)),
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_PHOTO, searchQuery=SearchQuery(mediaType=SearchQuery.MediaType.PHOTO)),
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_MOVIE, searchQuery=SearchQuery(mediaType=SearchQuery.MediaType.VIDEO)),
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST),
            MenuBarItem(MenuBarItem.MenuBarItemType.SETTING),
        )
        _allItemList[MenuBarType.ALBUM_LIST] = listOf()
        _allItemList[MenuBarType.YEAR_LIST] = listOf()

        viewModelScope.launch {
            _accountState.photoRepository.collect() {
                if(_accountState.photoRepository.value != null) {
                    val albumList = _accountState.photoRepository.value!!.getAlbumList()

                    _allItemList[MenuBarType.ALBUM_LIST] = albumList.map { album ->
                        MenuBarItem(
                            MenuBarItem.MenuBarItemType.ALBUM_ITEM,
                            album,
                            SearchQuery(album=album)
                        )
                    }

                    if(_selectedMenuBarType == MenuBarType.ALBUM_LIST) {
                        _itemList.emit(_allItemList[_selectedMenuBarType]!!)
                    }
                }
            }
        }

        for(menuType in MenuBarType.values()){
            _selectedItemIndex[menuType] = 0
        }
    }

    fun loadIcon(menuBarItem: MenuBarItem, width:Int?, height:Int?, callback:(bmp:Bitmap?)->Unit) {
        if(_accountState.photoRepository.value != null) {
            viewModelScope.launch {
                if (_accountState.photoRepository.value != null && menuBarItem.album != null) {
                    val bmp = _accountState.photoRepository.value!!.getAlbumCoverPhoto(
                        menuBarItem.album,
                        width,
                        height,
                        true
                    )
                    callback.invoke(bmp)
                }
            }
        }
    }
}
