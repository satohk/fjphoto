package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject


class MenuBarViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)
    private val _allItemList = mutableMapOf<MenuType, List<MenuBarItem>>()
    private val _selectedItemIndex = mutableMapOf<MenuType, Int>()
    private var _selectedMenuType: MenuType = MenuType.TOP
    var selectedMenuType: MenuType
        get() = _selectedMenuType
        set(value){ _selectedMenuType = value }

    private val _albumListLoaded = MutableStateFlow<Boolean>(false)
    val albumListLoaded: StateFlow<Boolean> get() = _albumListLoaded

    val itemList: List<MenuBarItem>
        get() = _allItemList[_selectedMenuType]!!
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

        viewModelScope.launch(Dispatchers.IO)  {
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
                    _albumListLoaded.emit(true)
                }
            }

            _albumListLoaded.emit(true)
        }

        for(menuType in MenuType.values()){
            _selectedItemIndex[menuType] = 0
        }
    }

    suspend fun loadIcon(menuBarItem: MenuBarItem, width:Int?, height:Int?): Bitmap?{
        if(_accountState.photoRepository.value != null && menuBarItem.album != null) {
            return _accountState.photoRepository.value!!.getAlbumCoverPhoto(
                menuBarItem.album,
                width,
                height,
                true
            )
        }
        else{
            return null
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
        companion object {
            /* DiffUtilの定義 */
            val DIFF_UTIL = object: DiffUtil.ItemCallback<MenuBarItem>() {
                override fun areItemsTheSame(oldItem: MenuBarItem, newItem: MenuBarItem)
                        : Boolean {
                    return oldItem.itemType == newItem.itemType && oldItem.itemId == newItem.itemId
                }

                override fun areContentsTheSame(oldItem: MenuBarItem, newItem: MenuBarItem)
                        : Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}
