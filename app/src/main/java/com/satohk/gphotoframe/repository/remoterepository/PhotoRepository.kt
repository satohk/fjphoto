package com.satohk.gphotoframe.repository.remoterepository

import android.graphics.Bitmap
import com.satohk.gphotoframe.repository.entity.Album
import com.satohk.gphotoframe.repository.entity.SearchQueryRemote
import com.satohk.gphotoframe.repository.entity.PhotoMetadataRemote

typealias PhotoRequestHeader = Map<String, String>

interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoBitmap(photo: PhotoMetadataRemote, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap?
    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap?
    suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryRemote?):Pair<List<PhotoMetadataRemote>,String?>
    fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRemote): Pair<PhotoRequestHeader, String>

    fun getCategoryList(): List<String>
}