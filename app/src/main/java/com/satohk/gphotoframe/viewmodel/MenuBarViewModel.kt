package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


data class MenuBarItem(
    val itemType: MenuBarItemType,
    val action: SideBarAction,
    val album: Album? = null,
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

class MenuBarViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)
    private val _itemList = MutableStateFlow(listOf<MenuBarItem>())
    val itemList: StateFlow<List<MenuBarItem>> get() = _itemList

    private val _sideBarAction: MutableStateFlow<SideBarAction?> = MutableStateFlow(null)
    val sideBarAction: StateFlow<SideBarAction?> get() = _sideBarAction

    var lastFocusIndex: Int = 0
        private set

    fun initItemList(sideBarType: SideBarType) {
        _itemList.value = listOf()
        viewModelScope.launch {
            when (sideBarType) {
                SideBarType.TOP -> {
                    _itemList.emit(
                        listOf(
                            MenuBarItem(
                                MenuBarItem.MenuBarItemType.SHOW_ALL,
                                SideBarAction(SideBarActionType.ENTER_GRID,
                                            gridContents=GridContents())
                            ),
                            MenuBarItem(
                                MenuBarItem.MenuBarItemType.SHOW_PHOTO,
                                SideBarAction(SideBarActionType.ENTER_GRID,
                                    gridContents=GridContents(searchQuery=SearchQuery(mediaType=MediaType.PHOTO)))
                            ),
                            MenuBarItem(
                                MenuBarItem.MenuBarItemType.SHOW_MOVIE,
                                SideBarAction(SideBarActionType.ENTER_GRID,
                                    gridContents=GridContents(searchQuery=SearchQuery(mediaType=MediaType.VIDEO)))
                            ),
                            MenuBarItem(
                                MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST,
                                SideBarAction(SideBarActionType.CHANGE_SIDEBAR,
                                    sideBarType=SideBarType.ALBUM_LIST)
                            ),
                            MenuBarItem(
                                MenuBarItem.MenuBarItemType.SETTING,
                                SideBarAction(SideBarActionType.CHANGE_SIDEBAR,
                                    sideBarType=SideBarType.SETTING)
                            ),
                        )
                    )
                }
                SideBarType.ALBUM_LIST -> {
                    if (_accountState.photoRepository.value != null) {
                        val albumList = _accountState.photoRepository.value!!.getAlbumList()

                        _itemList.emit(
                            albumList.map { album ->
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.ALBUM_ITEM,
                                    SideBarAction(SideBarActionType.ENTER_GRID,
                                        gridContents=GridContents()),
                                    album=album
                                )
                            }
                        )
                    }
                    else{
                        Log.d("loadNextImageList", "_accountState.photoRepository.value is null")
                    }
                }
            }
        }
    }

    fun onClickMenuItem(itemIndex: Int) {
        viewModelScope.launch {
            _sideBarAction.emit(_itemList.value[itemIndex].action)
        }
    }

    fun onFocusMenuItem(itemIndex: Int) {
        lastFocusIndex = itemIndex

        val action = _itemList.value[itemIndex].action
        if(action.actionType == SideBarActionType.ENTER_GRID) {
            // グリッドの表示を更新するボタンにフォーカスしたときは、グリッドの表示を変更
            val focusAction = SideBarAction(
                SideBarActionType.CHANGE_GRID,
                gridContents=action.gridContents
            )

            viewModelScope.launch {
                _sideBarAction.emit(focusAction)
            }
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _sideBarAction.emit(SideBarAction(SideBarActionType.BACK))
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
