package com.satohk.gphotoframe.repository

import android.graphics.Bitmap
import com.satohk.gphotoframe.model.Album
import com.satohk.gphotoframe.model.PhotoMetadataRepo
import com.satohk.gphotoframe.model.SearchQueryRepo

typealias PhotoRequestHeader = Map<String, String>

interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoBitmap(photo: PhotoMetadataRepo, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap?
    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap?
    suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryRepo?):Pair<List<PhotoMetadataRepo>,String?>
    fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRepo): Pair<PhotoRequestHeader, String>

    fun getCategoryList(): List<String>
}