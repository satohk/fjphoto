package com.satohk.gphotoframe.model

import java.time.ZonedDateTime
import kotlinx.serialization.Serializable

typealias PhotoCategory = List<String>

data class PhotoMetadata(
    val timestamp: ZonedDateTime,
    val id: String,
    val url: String
)

@Serializable
enum class MediaType{
    ALL,
    PHOTO,
    VIDEO
}

@Serializable
data class Album(
    val id: String,
    val name: String,
    val coverPhotoUrl: String?
)

@Serializable
data class SearchQuery (
    val album: Album? = null,
    val photoCategory: PhotoCategory? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val startDate: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val endDate: ZonedDateTime? = null,
    val mediaType:MediaType = MediaType.ALL,
)
