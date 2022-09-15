package com.satohk.gphotoframe.repository

import android.graphics.Bitmap
import com.satohk.gphotoframe.model.Album
import com.satohk.gphotoframe.model.PhotoMetadata
import com.satohk.gphotoframe.model.SearchQueryForRepo

typealias PhotoRequestHeader = Map<String, String>

interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoBitmap(photo: PhotoMetadata, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap?
    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap?
    suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryForRepo?):Pair<List<PhotoMetadata>,String?>
    fun getMediaAccessHeaderAndUrl(media: PhotoMetadata): Pair<PhotoRequestHeader, String>

    fun getCategoryList(): List<String>
}