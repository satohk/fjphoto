package com.satohk.gphotoframe.viewmodel

import com.satohk.gphotoframe.model.Album
import com.satohk.gphotoframe.model.SearchQuery


enum class SideBarType{
    MENUBAR,
    SEARCH,
    SETTING
}

enum class MenuBarType{
    TOP,
    ALBUM_LIST,
    YEAR_LIST,
    NONE
}

data class SideBarMenuBarType(
    val sideBarType: SideBarType = SideBarType.MENUBAR,
    val menuBarType: MenuBarType = MenuBarType.TOP
){
    companion object{
        val DEFAULT_VALUE = SideBarMenuBarType()
    }
}

data class MenuBarItem(
    val itemType: MenuBarItemType,
    val album: Album? = null,
    val searchQuery: SearchQuery? = null
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