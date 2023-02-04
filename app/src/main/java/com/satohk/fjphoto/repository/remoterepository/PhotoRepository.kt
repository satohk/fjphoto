package com.satohk.fjphoto.repository.remoterepository

import android.graphics.Bitmap
import com.satohk.fjphoto.repository.data.Album
import com.satohk.fjphoto.repository.data.SearchQueryRemote
import com.satohk.fjphoto.repository.data.PhotoMetadataRemote

typealias PhotoRequestHeader = Map<String, String>

interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoBitmap(photo: PhotoMetadataRemote, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap?
    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap?
    suspend fun getPhotoMetadata(photoId: String):PhotoMetadataRemote
    suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryRemote?):Pair<List<PhotoMetadataRemote>,String?>
    fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRemote): Pair<PhotoRequestHeader, String>

    fun getCategoryList(): List<String>
}