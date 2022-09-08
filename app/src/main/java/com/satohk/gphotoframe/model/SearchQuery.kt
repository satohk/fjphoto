package com.satohk.gphotoframe.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchQueryForLocal(
    val aiFilterEnabled: Boolean = false,
    val aiFilterThreshold: Float = 0.0f,
    val aiFilterReferenceData: List<PhotoMetadata> = listOf()
)

@Serializable
data class SearchQuery (
    val queryForRepo: SearchQueryForRepo = SearchQueryForRepo(),
    val queryForLocal: SearchQueryForLocal = SearchQueryForLocal()
)