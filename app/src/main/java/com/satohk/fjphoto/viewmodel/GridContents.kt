package com.satohk.fjphoto.viewmodel

import com.satohk.fjphoto.repository.data.SearchQuery
import java.io.Serializable

data class GridContents(
    val searchQuery: SearchQuery
) : Serializable{}
