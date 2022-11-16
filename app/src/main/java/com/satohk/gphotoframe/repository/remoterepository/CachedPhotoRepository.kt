package com.satohk.gphotoframe.repository.remoterepository

import android.accounts.NetworkErrorException
import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.Math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.satohk.gphotoframe.repository.data.Album
import com.satohk.gphotoframe.repository.data.PhotoMetadataRemote
import com.satohk.gphotoframe.repository.data.SearchQueryRemote

class CachedPhotoRepository(
    private val _photoRepository: PhotoRepository
) {
    private val _errorOccured = MutableStateFlow(false)
    val errorOccured: StateFlow<Boolean> get() = _errorOccured
    fun setError(){
        _errorOccured.value = true
    }

    private class PhotoMetadataList(
        val pageToken: String?,
        val photoMetadataList: MutableList<PhotoMetadataRemote>,
        val allLoaded: Boolean
    ){
        val size: Int get() = photoMetadataList.size
    }

    private val _albumCache = mutableListOf<Album>()
    private val _photoBitmapCache = LruCache<String, Bitmap>(1024)
    private val _albumCoverCache = LruCache<String, Bitmap>(1024)
    private val _mutex = Mutex()

    // 全listの要素数の合計をmaxSize以下とする
    private val _photoMetadataCache = object : LruCache<String, PhotoMetadataList>(10000) {
        override fun sizeOf(key:String, metadataList: PhotoMetadataList):Int{
            return min(metadataList.size, this.maxSize())
        }
    }

    private fun arg2str(vararg args: Any?): String{
        var key: String = ""
        args.forEach {
            key += it.toString() + "_"
        }
        return key
    }

    fun photoMetadataListAllLoaded(searchQuery: SearchQueryRemote?):Boolean{
        val key = arg2str(searchQuery)
        return _photoMetadataCache.get(key).allLoaded
    }

    suspend fun getPhotoMetadataList(offset:Int, size:Int, searchQuery: SearchQueryRemote?):List<PhotoMetadataRemote>{
        val key = arg2str(searchQuery)
        var list = _photoMetadataCache.get(key)

        if((list == null) || (!list.allLoaded && list.size < offset + size)){
            try {
                _mutex.withLock {
                    val pageToken = list?.pageToken
                    val bulkLoadSize = 60
                    val res = _photoRepository.getNextPhotoMetadataList(
                        bulkLoadSize, pageToken, searchQuery
                    )
                    val resultList = if (list == null) mutableListOf() else list.photoMetadataList
                    resultList.addAll(res.first)
                    val nextPageToken = res.second
                    list = PhotoMetadataList(nextPageToken, resultList, nextPageToken == null)
                    _photoMetadataCache.put(key, list)
                }
            }
            catch(e: NetworkErrorException){
                setError()
            }
        }

        return if((list != null) && (list.size >= offset)){
            list.photoMetadataList.subList(offset, min(offset + size, list.size))
        } else{
            listOf()
        }
    }

    suspend fun getAlbumList():List<Album> {
        if(_albumCache.size == 0) {
            try {
                _albumCache.addAll(_photoRepository.getAlbumList())
            }
            catch(e: NetworkErrorException){
                setError()
            }
        }
        return _albumCache
    }

    suspend fun getPhotoBitmap(photo: PhotoMetadataRemote, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap? {
        val key = arg2str(photo, width, height, cropFlag)
        var res = _photoBitmapCache.get(key)
        if(res == null){
            try {
                res = _photoRepository.getPhotoBitmap(photo, width, height, cropFlag)
                if(res != null) {
                    _photoBitmapCache.put(key, res)
                }
            }
            catch(e: NetworkErrorException){
                setError()
            }
        }
        return res
    }

    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        val key = arg2str(album, width, height, cropFlag)
        var res = _albumCoverCache.get(key)
        if(res == null){
            try {
                res = _photoRepository.getAlbumCoverPhoto(album, width, height, cropFlag)
                _albumCoverCache.put(key, res)
            }
            catch(e: NetworkErrorException){
                setError()
            }
        }
        return res
    }

    fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRemote): Pair<PhotoRequestHeader, String> {
        return _photoRepository.getMediaAccessHeaderAndUrl(media)
    }

    fun getCategoryList(): List<String>{
        return _photoRepository.getCategoryList()
    }
}