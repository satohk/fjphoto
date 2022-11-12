package com.satohk.gphotoframe.repository.entity

import kotlinx.serialization.Serializable

@Serializable
data class Setting(
    val slideShowInterval: Int,
    val numPhotoGridColumns: Int,
    val screensaverSearchQuery: SearchQuery
)