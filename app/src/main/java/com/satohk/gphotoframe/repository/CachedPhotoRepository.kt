package com.satohk.gphotoframe.repository

import android.graphics.Bitmap
import android.util.LruCache
import com.satohk.gphotoframe.model.Album
import com.satohk.gphotoframe.model.PhotoMetadataRepo
import com.satohk.gphotoframe.model.SearchQueryRepo
import java.lang.Math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CachedPhotoRepository(
    private val _repository: PhotoRepository
) {
    private class PhotoMetadataList(
        val pageToken: String?,
        val photoMetadataList: MutableList<PhotoMetadataRepo>,
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
        override fun sizeOf(key:String, metadataList:PhotoMetadataList):Int{
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

    fun photoMetadataListAllLoaded(searchQuery:SearchQueryRepo?):Boolean{
        val key = arg2str(searchQuery)
        return _photoMetadataCache.get(key).allLoaded
    }

    suspend fun getPhotoMetadataList(offset:Int, size:Int, searchQuery:SearchQueryRepo?):List<PhotoMetadataRepo>{
        val key = arg2str(searchQuery)
        var list = _photoMetadataCache.get(key)

        if((list == null) || (!list.allLoaded && list.size < offset + size)){
            _mutex.withLock {
                val pageToken = list?.pageToken
                val bulkLoadSize = 60
                val res = _repository.getNextPhotoMetadataList(
                    bulkLoadSize, pageToken, searchQuery
                )
                val resultList = if(list == null) mutableListOf() else list.photoMetadataList
                resultList.addAll(res.first)
                val nextPageToken = res.second
                list = PhotoMetadataList(nextPageToken, resultList, nextPageToken == null)
                _photoMetadataCache.put(key, list)
            }
        }

        return if(list.size >= offset){
            list.photoMetadataList.subList(offset, min(offset + size, list.size))
        } else{
            listOf()
        }
    }

    suspend fun getAlbumList():List<Album> {
        if(_albumCache.size == 0) {
            _albumCache.addAll(_repository.getAlbumList())
        }
        return _albumCache
    }

    suspend fun getPhotoBitmap(photo: PhotoMetadataRepo, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap? {
        val key = arg2str(photo, width, height, cropFlag)
        var res = _photoBitmapCache.get(key)
        if(res == null){
            res = _repository.getPhotoBitmap(photo, width, height, cropFlag)
            if(res != null) {
                _photoBitmapCache.put(key, res)
            }
        }
        return res
    }

    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        val key = arg2str(album, width, height, cropFlag)
        var res = _albumCoverCache.get(key)
        if(res == null){
            res = _repository.getAlbumCoverPhoto(album, width, height, cropFlag)
            _albumCoverCache.put(key, res)
        }
        return res
    }

    fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRepo): Pair<PhotoRequestHeader, String> {
        return _repository.getMediaAccessHeaderAndUrl(media)
    }

    fun getCategoryList(): List<String>{
        return _repository.getCategoryList()
    }
}