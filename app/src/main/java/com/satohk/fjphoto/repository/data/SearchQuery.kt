package com.satohk.fjphoto.repository.data

import androidx.room.Index
import com.satohk.fjphoto.domain.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime


@Serializable
enum class MediaType{
    ALL,
    PHOTO,
    VIDEO
}

@Serializable
enum class OrderBy{
    CREATION_TIME_ASC,  // oldest first
    CREATION_TIME_DESC  // newest first
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
    val orderBy: OrderBy = OrderBy.CREATION_TIME_DESC
)

@Serializable
data class SearchQueryLocal(
    val aiFilterEnabled: Boolean = false,
    val aiFilterThreshold: Float = 0.0f,
    val aiFilterReferenceDataIdList: List<String> = listOf()
)

@Serializable
data class SearchQuery (
    val queryRemote: SearchQueryRemote = SearchQueryRemote(),
    val queryLocal: SearchQueryLocal = SearchQueryLocal(),
    val serviceProviderUrl: String? = null,
    val userName: String? = null
){
    constructor(copyFrom: SearchQuery, orderBy: OrderBy) : this(
        SearchQueryRemote(
            album = copyFrom.queryRemote.album,
            photoCategory = copyFrom.queryRemote.photoCategory,
            startDate = copyFrom.queryRemote.startDate,
            endDate = copyFrom.queryRemote.endDate,
            mediaType = copyFrom.queryRemote.mediaType,
            orderBy = orderBy
        ),
        copyFrom.queryLocal,
        copyFrom.serviceProviderUrl,
        copyFrom.userName
    )
}