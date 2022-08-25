package com.satohk.gphotoframe.model

import android.graphics.Bitmap
import android.util.LruCache

class CachedPhotoRepository(
    private val repository: PhotoRepository
) : PhotoRepository{

    private val albumCache = mutableListOf<Album>()
    private val photoBitmapCache = LruCache<String, Bitmap>(256)
    private val albumCoverCache = LruCache<String, Bitmap>(256)

    fun arg2str(vararg args: Any?): String{
        var key: String = ""
        args.forEach {
            key += it.toString() + "_"
        }
        return key
    }

    override suspend fun getAlbumList():List<Album> {
        if(albumCache.size == 0) {
            albumCache.addAll(repository.getAlbumList())
        }
        return albumCache
    }

    override suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery:SearchQuery?):Pair<List<PhotoMetadata>,String?>{
        return repository.getNextPhotoMetadataList(pageSize, pageToken, searchQuery)
    }

    override suspend fun getPhotoBitmap(photo:PhotoMetadata, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap? {
        val key = arg2str(photo, width, height, cropFlag)
        var res = photoBitmapCache.get(key)
        if(res == null){
            res = repository.getPhotoBitmap(photo, width, height, cropFlag)
            photoBitmapCache.put(key, res)
        }
        return res
    }

    override suspend fun getAlbumCoverPhoto(album:Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        val key = arg2str(album, width, height, cropFlag)
        var res = albumCoverCache.get(key)
        if(res == null){
            res = repository.getAlbumCoverPhoto(album, width, height, cropFlag)
            albumCoverCache.put(key, res)
        }
        return res
    }

    override fun getCategoryList(): List<String>{
        return repository.getCategoryList()
    }
}