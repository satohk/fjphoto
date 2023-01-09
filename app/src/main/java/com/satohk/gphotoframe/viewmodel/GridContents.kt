package com.satohk.gphotoframe.viewmodel

import com.satohk.gphotoframe.repository.data.SearchQuery
import java.io.Serializable

data class GridContents(
    val searchQuery: SearchQuery
) : Serializable{}
