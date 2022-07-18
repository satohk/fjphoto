package com.satohk.gphotoframe.viewmodel


data class SidebarAction(
    val actionType: SideBarActionType,
    val sideBarType: SideBarType? = null,
    val gridContents: GridContents? = null
){
}

enum class SideBarActionType{
    CHANGE_SIDEBAR,
    CHANGE_GRID,
    ENTER_GRID,
    BACK
}

enum class SideBarType{
    SEARCH,
    SETTING,
    TOP,
    ALBUM_LIST,
    YEAR_LIST,
}