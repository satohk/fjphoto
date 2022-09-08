package com.satohk.gphotoframe.model

import java.time.ZonedDateTime
import kotlinx.serialization.Serializable

@Serializable
data class PhotoMetadata(
    @Serializable(with = ZonedDateTimeSerializer::class)
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
data class SearchQueryForRepo (
    val album: Album? = null,
    val photoCategory: List<String>? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val startDate: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val endDate: ZonedDateTime? = null,
    val mediaType:MediaType = MediaType.ALL,
)

