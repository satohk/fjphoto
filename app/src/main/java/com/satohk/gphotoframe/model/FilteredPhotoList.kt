package com.satohk.gphotoframe.model

import android.util.Log
import com.satohk.gphotoframe.repository.CachedPhotoRepository
import kotlinx.coroutines.*

class FilteredPhotoList(
    private val _repository: CachedPhotoRepository,
    private val _query: SearchQuery
){
    private val _filteredPhotoMetadataList = mutableListOf<PhotoMetadataRepo>()
    private var _repositoryOffset = 0
    private var _preloadRepositoryOffset = 0
    private val _bulkLoadSize = 60
    private val _preloadPhotoSize = 256
    private val _scope = CoroutineScope(Job() + Dispatchers.Default)

    var allLoaded: Boolean = false
        private set

    val list: List<PhotoMetadataRepo> get() = this._filteredPhotoMetadataList

    suspend fun getFilteredPhotoMetadataList(offset:Int, size:Int):List<PhotoMetadataRepo>{
        var remain = offset + size - _filteredPhotoMetadataList.size

        _scope.launch {
            while(remain > 0){
                // bulkLoadSizeごとにあらかじめCacheにロードしておく
                if(_repositoryOffset >= _preloadRepositoryOffset) {
                    val preloadMetadataList = _repository.getPhotoMetadataList(
                        _preloadRepositoryOffset,
                        _bulkLoadSize,
                        _query.queryRepo
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
                    _query.queryRepo
                )

                if(metadataList.isEmpty() && _repository.photoMetadataListAllLoaded(_query.queryRepo)){
                    allLoaded = true
                }
                else {
                    val filterResult = metadataList.map { it ->
                        async { filterPhoto(it) }
                    }.awaitAll()
                    val filteredList =
                        metadataList.zip(filterResult).filter { it.second }.map { it.first }
                    _filteredPhotoMetadataList.addAll(filteredList)
                    remain -= filteredList.size
                    _repositoryOffset += metadataList.size
                }
            }
        }.join()

        Log.d("_filteredPhotoMetadataList.size", _filteredPhotoMetadataList.size.toString())

        return if(_filteredPhotoMetadataList.size < offset){
            listOf()
        } else {
            _filteredPhotoMetadataList.subList(offset, Math.min(offset + size, _filteredPhotoMetadataList.size))
        }
    }

    var ct = 0
    suspend private fun filterPhoto(photoMetadata: PhotoMetadataRepo):Boolean{
        return true

        _repository.getPhotoBitmap(photoMetadata, _preloadPhotoSize, _preloadPhotoSize, false)
        ct += 1
        return (ct % 2) == 0
    }
}