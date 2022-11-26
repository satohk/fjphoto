package com.satohk.gphotoframe.domain

import android.util.Log
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.data.PhotoMetadata
import com.satohk.gphotoframe.repository.data.PhotoMetadataLocal
import com.satohk.gphotoframe.repository.data.PhotoMetadataRemote
import com.satohk.gphotoframe.repository.data.SearchQuery
import kotlinx.coroutines.*

class FilteredPhotoList(
    private val _repository: CachedPhotoRepository,
    private val _query: SearchQuery
){
    private val _filteredPhotoMetadataList = mutableListOf<PhotoMetadata>()
    private var _repositoryOffset = 0
    private var _preloadRepositoryOffset = 0
    private val _bulkLoadSize = 60
    private val _preloadPhotoSize = 256
    private val _scope = CoroutineScope(Job() + Dispatchers.Default)

    var allLoaded: Boolean = false
        private set

    val size: Int get() = this._filteredPhotoMetadataList.size

    operator fun get(i:Int): PhotoMetadata{
        return _filteredPhotoMetadataList[i]
    }
    operator fun set(i:Int, value:PhotoMetadata){
        _filteredPhotoMetadataList[i] = value
    }

    suspend fun loadNext(size:Int) {
        var remain = size

        if(!allLoaded){
            _scope.launch {
                while(remain > 0){
                    // bulkLoadSizeごとにあらかじめCacheにロードしておく
                    if(_repositoryOffset >= _preloadRepositoryOffset) {
                        val preloadMetadataList = _repository.getPhotoMetadataList(
                            _preloadRepositoryOffset,
                            _bulkLoadSize,
                            _query.queryRemote
                        )
                        if (preloadMetadataList.isEmpty()) {
                            break
                        }
                        _preloadRepositoryOffset += preloadMetadataList.size
                    }

                    // Cacheから指定数ロード
                    val metadataList = _repository.getPhotoMetadataList(
                        _repositoryOffset,
                        remain,
                        _query.queryRemote
                    )

                    if(metadataList.isEmpty() && _repository.photoMetadataListAllLoaded(_query.queryRemote)){
                        allLoaded = true
                    }
                    else {
                        val filterResult = metadataList.map { it ->
                            async { filterPhoto(it) }
                        }.awaitAll()
                        val filteredList = metadataList.zip(filterResult).filter { it.second }.map { it.first }
                        _filteredPhotoMetadataList.addAll(filteredList)
                        remain -= filteredList.size
                        _repositoryOffset += metadataList.size
                    }
                }
            }.join()
        }
        Log.d("_filteredPhotoMetadataList.size", _filteredPhotoMetadataList.size.toString())
    }

    var ct = 0
    suspend private fun filterPhoto(photoMetadata: PhotoMetadata):Boolean{
        return true

        _repository.getPhotoBitmap(photoMetadata.metadataRemote, _preloadPhotoSize, _preloadPhotoSize, false)
        ct += 1
        return (ct % 2) == 0
    }
}