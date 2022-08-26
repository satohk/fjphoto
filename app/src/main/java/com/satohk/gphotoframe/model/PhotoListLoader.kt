package com.satohk.gphotoframe.model

import com.satohk.gphotoframe.repository.PhotoRepository

class PhotoListLoader(
    private val _repository: PhotoRepository,
    private val _searchQuery: SearchQuery
){
    private val _photoList = mutableListOf<PhotoMetadata>()
    private var _pageToken: String? = null


}