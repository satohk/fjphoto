package com.satohk.gphotoframe.model

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime


// albums.list response

@Serializable
internal data class SharedAlbumOptions(
    val isCollaborative: Boolean? = null,
    val isCommentable: Boolean? = null
)

@Serializable
internal data class ShareInfo(
    val sharedAlbumOptions: SharedAlbumOptions? = null,
    val shareableUrl: String? = null,
    val shareToken: String? = null,
    val isJoined: Boolean? = null,
    val isOwned: Boolean? = null,
    val isJoinable: Boolean? = null
)

@Serializable
internal data class GooglePhotoAlbum(
    val id: String,
    val title: String,
    val productUrl: String,
    val isWriteable: Boolean? = null,
    val shareInfo: ShareInfo? = null,
    val mediaItemsCount: String? = null,
    val coverPhotoBaseUrl: String? = null,
    val coverPhotoMediaItemId: String? = null
)

@Serializable
internal data class AlbumsResponse(
    val albums: List<GooglePhotoAlbum>,
    val nextPageToken: String? = null
)


// mediaItems.search param

@Serializable
internal data class ParamDate(
    val year: Int,
    val month: Int,
    val day: Int
)
{
    constructor(date: ZonedDateTime): this(date.year, date.monthValue, date.dayOfMonth)
}

@Serializable
internal data class ParamDateRange(
    val startDate: ParamDate,
    val endDate: ParamDate
)

@Serializable
internal data class ParamDateFilter(
    val dates: List<ParamDate>? = null,
    val ranges: List<ParamDateRange>? = null
)

@Serializable
internal data class ParamContentFilter(
    val includedContentCategories: List<ParamContentCategory>? = null,
    val excludedContentCategories: List<ParamContentCategory>? = null
)

@Serializable
internal enum class ParamMediaType{
    ALL_MEDIA,
    VIDEO,
    PHOTO
}

@Serializable
internal enum class ParamContentCategory{
    NONE,
    LANDSCAPES,
    RECEIPTS,
    CITYSCAPES,
    LANDMARKS,
    SELFIES,
    PEOPLE,
    PETS,
    WEDDINGS,
    BIRTHDAYS,
    DOCUMENTS,
    TRAVEL,
    ANIMALS,
    FOOD,
    SPORT,
    NIGHT,
    PERFORMANCES,
    WHITEBOARDS,
    SCREENSHOTS,
    UTILITY,
    ARTS,
    CRAFTS,
    FASHION,
    HOUSES,
    GARDENS,
    FLOWERS,
    HOLIDAYS,
}

@Serializable
internal data class ParamMediaTypeFilter(
    val mediaTypes: List<ParamMediaType>? = null,
){
    constructor(mediaType: ParamMediaType) : this(listOf(mediaType)){
    }
}

@Serializable
internal data class ParamFilters(
    val dateFilter: ParamDateFilter? = null,
    val contentFilter: ParamContentFilter? = null,
    val mediaTypeFilter: ParamMediaTypeFilter? = null,
//  val featureFilter: ParamFeatureFilter,
//  val includeArchivedMedia: Boolean,
//  val excludeNonAppCreatedData: Boolean
)

@Serializable
internal data class SearchParam(
    val albumId: String? = null,
    val pageSize: Int,
    val pageToken: String? = null,
    val filters: ParamFilters? = null
)

// mediaItems response
@Serializable
internal data class Photo(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val focalLength: Float? = null,
    val apertureFNumber: Float? = null,
    val isoEquivalent: Int? = null,
    val exposureTime: String? = null
)

@Serializable
internal data class Video(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val fps: Float? = null,
    val status: String? = null
)

@Serializable
internal data class MediaMetadata(
    val creationTime: String? = null,
    val width: String? = null,
    val height: String? = null,
    val photo: Photo? = null,
    val video: Video? = null
)

@Serializable
internal data class ContributorInfo(
    val profilePictureBaseUrl: String? = null,
    val displayName: String? = null
)

@Serializable
internal data class MediaItem(
    val id: String,
    val description: String? = null,
    val productUrl: String,
    val baseUrl: String,
    val mimeType: String? = null,
    val mediaMetadata: MediaMetadata? = null,
    val contributorInfo: ContributorInfo? = null,
    val filename: String? = null
)

@Serializable
internal data class MediaItemsResponse(
    val mediaItems: List<MediaItem>? = null,
    val nextPageToken: String? = null
)
