package com.satohk.gphotoframe.repository.data

import com.satohk.gphotoframe.domain.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime


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
data class SearchQueryRemote (
    val album: Album? = null,
    val photoCategory: List<String>? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val startDate: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val endDate: ZonedDateTime? = null,
    val mediaType: MediaType = MediaType.ALL,
)

@Serializable
data class SearchQueryLocal(
    val aiFilterEnabled: Boolean = false,
    val aiFilterThreshold: Float = 0.0f,
    val aiFilterReferenceDataUrlList: List<String> = listOf()
)

@Serializable
data class SearchQuery (
    val queryRemote: SearchQueryRemote = SearchQueryRemote(),
    val queryLocal: SearchQueryLocal = SearchQueryLocal()
)