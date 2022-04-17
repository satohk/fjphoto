package com.satohk.gphotoframe.model

import java.time.ZonedDateTime

data class SearchQuery (
    val album: Album? = null,
    val photoCategory: PhotoCategory? = null,
    val startDate: ZonedDateTime? = null,
    val endDate: ZonedDateTime? = null,
    val mediaType:SearchQuery.MediaType = MediaType.ALL,
){
    enum class MediaType{
        ALL,
        PHOTO,
        VIDEO
    }
}
