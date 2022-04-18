package com.satohk.gphotoframe.viewmodel

import com.satohk.gphotoframe.model.Album
import com.satohk.gphotoframe.model.SearchQuery


enum class MenuBarType{
    TOP,
    ALBUM_LIST,
    YEAR_LIST
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