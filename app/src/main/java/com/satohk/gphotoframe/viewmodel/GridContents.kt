package com.satohk.gphotoframe.viewmodel

import com.satohk.gphotoframe.model.SearchQuery
import java.io.Serializable

data class GridContents(
    val searchQuery: SearchQuery = SearchQuery()
) : Serializable{}
