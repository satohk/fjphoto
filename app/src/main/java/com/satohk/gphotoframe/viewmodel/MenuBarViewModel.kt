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
    private val _allItemList = mutableMapOf<MenuType, List<MenuBarItem>>()
    private val _selectedItemIndex = mutableMapOf<MenuType, Int>()
    private var _selectedMenuType: MenuType = MenuType.TOP
    var selectedMenuType: MenuType
        get() = _selectedMenuType
        set(value){
            _selectedMenuType = value
            if(_allItemList[_selectedMenuType] != null) {
                _itemList.value = _allItemList[_selectedMenuType]!!
            }
        }

    private val _itemList = MutableStateFlow(listOf<MenuBarItem>())
    val itemList: StateFlow<List<MenuBarItem>> get() = _itemList
    var selectedItemIndex: Int
        get() = _selectedItemIndex[_selectedMenuType]!!
        set(value) { _selectedItemIndex[_selectedMenuType] = value}

    init{
        _allItemList[MenuType.TOP] = listOf(
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_ALL),
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_PHOTO),
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_MOVIE),
            MenuBarItem(MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST),
            MenuBarItem(MenuBarItem.MenuBarItemType.SETTING),
        )
        _allItemList[MenuType.ALBUM_LIST] = listOf()
        _allItemList[MenuType.YEAR_LIST] = listOf()

        viewModelScope.launch {
            _accountState.photoRepository.collect() {
                if(_accountState.photoRepository.value != null) {
                    val albumList = _accountState.photoRepository.value!!.getAlbumList()

                    _allItemList[MenuType.ALBUM_LIST] = albumList.map { album ->
                        MenuBarItem(
                            MenuBarItem.MenuBarItemType.ALBUM_ITEM,
                            album.id,
                            album.name,
                            album
                        )
                    }

                    if(_selectedMenuType == MenuType.ALBUM_LIST) {
                        _itemList.emit(_allItemList[_selectedMenuType]!!)
                    }
                }
            }
        }

        for(menuType in MenuType.values()){
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

    enum class MenuType{
        TOP,
        ALBUM_LIST,
        YEAR_LIST
    }

    data class MenuBarItem(
        val itemType: MenuBarItemType,
        val itemId: String? = null,
        val caption: String? = null,
        val album: Album? = null
    ) {
        enum class MenuBarItemType{
            SHOW_ALL,
            SHOW_PHOTO,
            SHOW_MOVIE,
            SHOW_ALBUM_LIST,
            SEARCH,
            SETTING,
            ALBUM_ITEM
        }
    }
}
