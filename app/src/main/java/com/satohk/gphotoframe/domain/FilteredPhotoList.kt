package com.satohk.gphotoframe.domain

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.data.PhotoMetadata
import com.satohk.gphotoframe.repository.data.PhotoMetadataRemote
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
    private val _scoreCache = ScoreCache(_visualInspector)

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
            _scoreCache.setParameter(_repository)

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
                        _scoreCache.setAnchorImages(_query.queryLocal.aiFilterReferenceDataIdList)
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
                        val filterResult = metadataList.map{
                            filterPhoto(it)
                        }

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

    private suspend fun filterPhoto(metadata: PhotoMetadata):Boolean{
        return if(_query.queryLocal.aiFilterEnabled){
            val score = _scoreCache.get(metadata.metadataRemote)
            score > _query.queryLocal.aiFilterThreshold
        } else {
            true
        }
    }

    class ScoreCache(private val _visualInspector: VisualInspector) {
        private lateinit var _repository: CachedPhotoRepository
        private val _cache = LruCache<String, Float>(1024*1024)
        private var _anchorIdList: List<String>? = null

        fun setParameter(repository: CachedPhotoRepository){
            _repository = repository
        }

        suspend fun setAnchorImages(idList:List<String>){
            val images = mutableListOf<Bitmap>()
            for(photoId in idList){
                val metadata = _repository.getPhotoMetadata(photoId)
                Log.d("initVisualInspectorAnchor", metadata.toString())
                val image = loadBmp(metadata)
                image?.let{ images.add(it) }
            }
            _visualInspector.setAnchorImage(images)

            if(!idList.equals(_anchorIdList)){
                // clear cache
                _cache.evictAll()
            }
            _anchorIdList = idList
        }

        suspend fun get(metadata:PhotoMetadataRemote): Float {
            var score = _cache.get(metadata.id)
            if(score == null){
                val bmp = loadBmp(metadata)
                score = if(bmp == null){
                    10000f
                } else {
                    _visualInspector.calcImageScore(bmp)
                }
                _cache.put(metadata.id, score)
            }
            return score
        }

        private suspend fun loadBmp(metadata: PhotoMetadataRemote): Bitmap?{
            return _repository.getPhotoBitmap(
                metadata,
                _visualInspector.inputImageSize.width,
                _visualInspector.inputImageSize.height,
                true)
        }
    }
}