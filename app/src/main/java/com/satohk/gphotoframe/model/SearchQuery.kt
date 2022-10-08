package com.satohk.gphotoframe.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchQueryLocal(
    val aiFilterEnabled: Boolean = false,
    val aiFilterThreshold: Float = 0.0f,
    val aiFilterReferenceData: List<PhotoMetadataRepo> = listOf()
)

@Serializable
data class SearchQuery (
    val queryRepo: SearchQueryRepo = SearchQueryRepo(),
    val queryLocal: SearchQueryLocal = SearchQueryLocal()
)

data class PhotoMetadataLocal(
    val favorite: Boolean = false
)

data class PhotoMetadata(
    val metadataRepo: PhotoMetadataRepo,
    val metadataLocal: PhotoMetadataLocal
)