package com.satohk.gphotoframe.repository.entity

import com.satohk.gphotoframe.domain.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class PhotoMetadataLocal(
    val favorite: Boolean = false
)

@Serializable
data class PhotoMetadataRemote(
    @Serializable(with = ZonedDateTimeSerializer::class)
    val timestamp: ZonedDateTime,
    val id: String,
    val url: String,
    val productUrl: String,
    val mimeType: String
)

@Serializable
data class PhotoMetadata(
    val metadataLocal: PhotoMetadataLocal,
    val metadataRemote: PhotoMetadataRemote
)