package com.satohk.gphotoframe.domain

import android.graphics.Bitmap
import android.util.Log
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.data.PhotoMetadata
import com.satohk.gphotoframe.repository.data.SearchQuery
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FilteredPhotoList(
    private val _visualInspector: VisualInspector
){
    private lateinit var _repository: CachedPhotoRepository
    private lateinit var _query: SearchQuery
    private var _parameterChanged: Boolean = false
    private val _filteredPhotoMetadataList = mutableListOf<PhotoMetadata>()
    private var _repositoryOffset = 0
    private var _preloadRepositoryOffset = 0
    private val _bulkLoadSize = 60
    private val _loadingMutex = Mutex()
    private var _cancelLoad = false

    var allLoaded: Boolean = false
        private set

    val size: Int get() = this._filteredPhotoMetadataList.size

    suspend fun setParameter(repository: CachedPhotoRepository, query: SearchQuery){
        _cancelLoad = true
        _loadingMutex.withLock {
            _query = query
            _repository = repository
            _filteredPhotoMetadataList.clear()
            _repositoryOffset = 0
            _preloadRepositoryOffset = 0

            _parameterChanged = true
            _cancelLoad = false
        }
    }

    operator fun get(i:Int): PhotoMetadata{
        return _filteredPhotoMetadataList[i]
    }
    operator fun set(i:Int, value:PhotoMetadata){
        _filteredPhotoMetadataList[i] = value
    }

    private suspend fun initVisualInspectorAnchor(){
        val images = mutableListOf<Bitmap>()
        for(photoId in _query.queryLocal.aiFilterReferenceDataIdList){
            val metadata = _repository.getPhotoMetadata(photoId)
            Log.d("initVisualInspectorAnchor", metadata.toString())
            val image = _repository.getPhotoBitmap(
                metadata,
                _visualInspector.inputImageSize.width,
                _visualInspector.inputImageSize.height,
                true
            )
            image?.let{ images.add(it) }
        }
        _visualInspector.setAnchorImage(images)
    }

    suspend fun loadNext(size:Int): Boolean {
        Log.d("loadNext", "start")
        var remain = size

        if(allLoaded) {
            return false
        }
        Log.d("loadNext", "_mutex.isLocked == ${_loadingMutex.isLocked}")
        if(_loadingMutex.tryLock()){
            Log.d("loadNext", " in lock")
            withContext(Dispatchers.Default){
                if(_parameterChanged) {
                    _parameterChanged = false
                    if(_query.queryLocal.aiFilterEnabled) {
                        initVisualInspectorAnchor()
                    }
                }

                while(remain > 0 && !_cancelLoad){
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
                        // load bmp async
                        val bmpList = metadataList.map { it ->
                            async {
                                _repository.getPhotoBitmap(
                                    it.metadataRemote,
                                    _visualInspector.inputImageSize.width,
                                    _visualInspector.inputImageSize.height,
                                    true)}
                        }.awaitAll()

                        // ai filter sync
                        Log.d("loadNext", "begin filter")
                        val filterResult = bmpList.map {
                            filterPhoto(it!!)
                        }
                        Log.d("loadNext", "end filter")

                        val filteredList = metadataList.zip(filterResult).filter { it.second }.map { it.first }
                        _filteredPhotoMetadataList.addAll(filteredList)
                        remain -= filteredList.size
                        _repositoryOffset += metadataList.size
                    }
                }
            }
            _loadingMutex.unlock()
            Log.d("loadNext", " end lock _mutex.isLocked == ${_loadingMutex.isLocked}")
            return true
        }
        else{
            return false
        }
    }

    private fun filterPhoto(bmp: Bitmap):Boolean{
        return if(_query.queryLocal.aiFilterEnabled){
            val score = _visualInspector.calcImageScore(bmp)
            Log.d("filterPhoto",  "score=${score}, threshold=${_query.queryLocal.aiFilterThreshold}")
            score > _query.queryLocal.aiFilterThreshold
        } else {
            true
        }
    }
}