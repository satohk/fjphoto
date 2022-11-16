package com.satohk.gphotoframe.repository.data

data class Setting(
    val slideShowInterval: Int = 10,
    val numPhotoGridColumns: Int = 6,
    val screensaverSearchQuery: SearchQuery = SearchQuery()
)