package com.satohk.gphotoframe.repository

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.satohk.gphotoframe.model.Album
import com.satohk.gphotoframe.model.PhotoMetadata
import com.satohk.gphotoframe.model.SearchQuery
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import java.lang.Math.min

class CachedPhotoRepository(
    private val _repository: PhotoRepository
) {
    private class PhotoMetadataList(
        val pageToken: String?,
        val photoMetadataList: List<PhotoMetadata>,
        val allLoaded: Boolean
    ){
        val size: Int get() = photoMetadataList.size
    }

    private val _albumCache = mutableListOf<Album>()
    private val _photoBitmapCache = LruCache<String, Bitmap>(256)
    private val _albumCoverCache = LruCache<String, Bitmap>(256)

    // 全listの要素数の合計をmaxSize以下とする
    private val _photoMetadataCache = object : LruCache<String, PhotoMetadataList>(1024) {
        override fun sizeOf(key:String, metadataList:PhotoMetadataList):Int{
            return metadataList.size
        }
    }

    private fun arg2str(vararg args: Any?): String{
        var key: String = ""
        args.forEach {
            key += it.toString() + "_"
        }
        return key
    }

    suspend fun getPhotoMetadataList(offset:Int, size:Int, searchQuery:SearchQuery?):List<PhotoMetadata>{
        val key = arg2str(searchQuery)
        var list = _photoMetadataCache.get(key)

        if((list == null) || (!list.allLoaded && list.size < offset + size)){
            val pageToken = list?.pageToken
            val loadSize = offset + size - (list?.size ?: 0)
            val result = _repository.getNextPhotoMetadataList(
                loadSize, pageToken, searchQuery
            )
            val resultList = if(list == null) result.first else list.photoMetadataList + result.first
            val nextPageToken = result.second
            list = PhotoMetadataList(nextPageToken, resultList, nextPageToken == null)
            _photoMetadataCache.put(key, list)
        }

        if(list.size < offset){
            return listOf()
        }
        else {
            return list.photoMetadataList.subList(offset, min(offset + size, list.size))
        }
    }

    suspend fun getAlbumList():List<Album> {
        if(_albumCache.size == 0) {
            _albumCache.addAll(_repository.getAlbumList())
        }
        return _albumCache
    }

    suspend fun getPhotoBitmap(photo: PhotoMetadata, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap? {
        val key = arg2str(photo, width, height, cropFlag)
        var res = _photoBitmapCache.get(key)
        if(res == null){
            res = _repository.getPhotoBitmap(photo, width, height, cropFlag)
            _photoBitmapCache.put(key, res)
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

    fun getCategoryList(): List<String>{
        return _repository.getCategoryList()
    }
}