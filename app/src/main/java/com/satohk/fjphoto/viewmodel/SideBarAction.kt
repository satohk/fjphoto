package com.satohk.fjphoto.viewmodel


data class SideBarAction(
    val actionType: SideBarActionType,
    val sideBarType: SideBarType? = null,
    val gridContents: GridContents? = null
){
}

enum class SideBarActionType{
    CHANGE_SIDEBAR,
    CHANGE_GRID,
    ENTER_GRID,
    LEAVE_GRID,
    BACK
}

enum class SideBarType{
    SEARCH,
    SETTING,
    TOP,
    ALBUM_LIST,
    YEAR_LIST,
}
