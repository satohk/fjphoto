package com.satohk.fjphoto.repository.data

data class Setting(
    val slideShowInterval: Int = 10,
    val slideShowOrder: Int = 0,
    val slideShowMute: Boolean = false,
    val slideShowCutPlay: Boolean = true,
    val numPhotoGridColumns: Int = 6,
    val screensaverSearchQuery: SearchQuery = SearchQuery()
)