package com.satohk.gphotoframe.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchQueryForLocal(
    val aiFilterEnabled: Boolean = false,
    val aiFilterThreshold: Float = 0.0f,
    val aiFilterReferenceData: List<PhotoMetadataFromRepo> = listOf()
)

@Serializable
data class SearchQuery (
    val queryRepo: SearchQueryForRepo = SearchQueryForRepo(),
    val queryLocal: SearchQueryForLocal = SearchQueryForLocal()
)

@Serializable
data class PhotoMetadataFromLocal(
    val favorite: Boolean = false
)

@Serializable
data class PhotoMetadata(
    val metadataRepo: PhotoMetadataFromRepo,
    val metadataLocal: PhotoMetadataFromLocal
)