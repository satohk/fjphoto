package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.*
import com.satohk.gphotoframe.repository.data.Album
import com.satohk.gphotoframe.repository.data.MediaType
import com.satohk.gphotoframe.repository.data.SearchQuery
import com.satohk.gphotoframe.repository.data.SearchQueryRemote
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MenuBarViewModel(
    private val _accountState: AccountState
    ) : SideBarActionPublisherViewModel() {

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

    private val _itemList = MutableStateFlow(listOf<MenuBarItem>())
    val itemList: StateFlow<List<MenuBarItem>> get() = _itemList

    private val _lastFocusIndexForSideBarType: MutableMap<SideBarType, Int> = mutableMapOf()
    var sideBarType: SideBarType = SideBarType.TOP
        private set
    val lastFocusIndex: Int
        get() = _lastFocusIndexForSideBarType[sideBarType]!!

    private val _activeUserName = MutableStateFlow<String?>(null)
    val activeUserName: StateFlow<String?> get() = _activeUserName


    init {
        _lastFocusIndexForSideBarType[SideBarType.TOP] = 0
        _lastFocusIndexForSideBarType[SideBarType.ALBUM_LIST] = 0

        viewModelScope.launch{
            _accountState.activeAccount.collect { account ->
                _activeUserName.value = account?.userName
            }
        }
    }

    fun initItemList(sideBarType: SideBarType) {
        this.sideBarType = sideBarType
        _itemList.value = listOf()
        viewModelScope.launch {
            _accountState.activeAccount.collect { account ->
                when (sideBarType) {
                    SideBarType.TOP -> {
                        _itemList.emit(
                            listOf(
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.SHOW_ALL,
                                    SideBarAction(
                                        SideBarActionType.ENTER_GRID,
                                        gridContents = GridContents(
                                            searchQuery = SearchQuery(
                                                userName = account?.userName,
                                                serviceProviderUrl = account?.serviceProviderUrl
                                            )
                                        )
                                    )
                                ),
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.SHOW_PHOTO,
                                    SideBarAction(
                                        SideBarActionType.ENTER_GRID,
                                        gridContents = GridContents(
                                            searchQuery = SearchQuery(
                                                SearchQueryRemote(mediaType = MediaType.PHOTO),
                                                userName = account?.userName,
                                                serviceProviderUrl = account?.serviceProviderUrl
                                            )
                                        )
                                    )
                                ),
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.SHOW_MOVIE,
                                    SideBarAction(
                                        SideBarActionType.ENTER_GRID,
                                        gridContents = GridContents(
                                            searchQuery = SearchQuery(
                                                SearchQueryRemote(mediaType = MediaType.VIDEO),
                                                userName = account?.userName,
                                                serviceProviderUrl = account?.serviceProviderUrl
                                            )
                                        )
                                    )
                                ),
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.SHOW_ALBUM_LIST,
                                    SideBarAction(
                                        SideBarActionType.CHANGE_SIDEBAR,
                                        sideBarType = SideBarType.ALBUM_LIST
                                    )
                                ),
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.SEARCH,
                                    SideBarAction(
                                        SideBarActionType.CHANGE_SIDEBAR,
                                        sideBarType = SideBarType.SEARCH
                                    )
                                ),
                                MenuBarItem(
                                    MenuBarItem.MenuBarItemType.SETTING,
                                    SideBarAction(
                                        SideBarActionType.CHANGE_SIDEBAR,
                                        sideBarType = SideBarType.SETTING
                                    )
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
                                        SideBarAction(
                                            SideBarActionType.ENTER_GRID,
                                            gridContents = GridContents(
                                                searchQuery = SearchQuery(
                                                    SearchQueryRemote(album = album),
                                                    userName = account?.userName,
                                                    serviceProviderUrl = account?.serviceProviderUrl
                                                )
                                            )
                                        ),
                                        album = album
                                    )
                                }
                            )
                        } else {
                            Log.d(
                                "loadNextImageList",
                                "_accountState.photoRepository.value is null"
                            )
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    fun enterToGrid(itemIndex: Int) {
        publishAction(_itemList.value[itemIndex].action)
    }

    fun goBack(){
        val action = SideBarAction(
            SideBarActionType.BACK,
            gridContents = null
        )
        publishAction(action)
    }

    fun changeFocus(itemIndex: Int) {
        Log.d("MenuBarViewModel", "lastFocusIndex " + lastFocusIndex.toString() + " itemIndex" + itemIndex.toString())
        _lastFocusIndexForSideBarType[sideBarType] = itemIndex

        val action = _itemList.value[itemIndex].action
        if(action.actionType == SideBarActionType.ENTER_GRID) {
            // グリッドの表示を更新するボタンにフォーカスしたときは、グリッドの表示を変更
            val focusAction = SideBarAction(
                SideBarActionType.CHANGE_GRID,
                gridContents=action.gridContents
            )
            publishAction(focusAction)
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

    fun changeAccount(){
        _accountState.setAccount(null)
    }
}
