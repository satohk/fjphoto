package com.satohk.gphotoframe.domain

import android.graphics.Bitmap
import android.util.Log
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.data.PhotoMetadata
import com.satohk.gphotoframe.repository.data.PhotoMetadataRemote
import com.satohk.gphotoframe.repository.data.SearchQuery
import kotlinx.coroutines.*
import org.koin.java.KoinJavaComponent
import java.time.ZonedDateTime

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

    private val _visualInspector: VisualInspector by KoinJavaComponent.inject(VisualInspector::class.java)
    private var _visualInspectorAnchorInitialized = false

    var allLoaded: Boolean = false
        private set

    val size: Int get() = this._filteredPhotoMetadataList.size

    operator fun get(i:Int): PhotoMetadata{
        return _filteredPhotoMetadataList[i]
    }
    operator fun set(i:Int, value:PhotoMetadata){
        _filteredPhotoMetadataList[i] = value
    }

    private suspend fun initVisualInspectorAnchor(){
        val images = mutableListOf<Bitmap>()
        for(url in _query.queryLocal.aiFilterReferenceDataUrlList){
            val image = _repository.getPhotoBitmap(
                PhotoMetadataRemote(ZonedDateTime.now(), "", url, "", ""),
                _visualInspector.inputImageSize.width,
                _visualInspector.inputImageSize.width,
                true
            )
            image?.let{ images.add(it) }
        }
        _visualInspector.setAnchorImage(images)
    }

    suspend fun loadNext(size:Int) {
        var remain = size

        if(!allLoaded){
            _scope.launch {
                if(!_visualInspectorAnchorInitialized){
                    initVisualInspectorAnchor()
                    _visualInspectorAnchorInitialized = true
                }

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
                            async {
                                val bmp = _repository.getPhotoBitmap(it.metadataRemote, _preloadPhotoSize, _preloadPhotoSize, true)
                                filterPhoto(it, bmp!!) }
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

    private fun filterPhoto(photoMetadata: PhotoMetadata, bmp: Bitmap):Boolean{
        return if(_query.queryLocal.aiFilterEnabled){
            val score = _visualInspector.calcImageScore(bmp)
            Log.d("filterPhoto score=", score.toString())
            score > 2    // test
            //!photoMetadata.metadataLocal.favorite
        } else {
            true
        }
    }
}