package com.satohk.gphotoframe.model

import android.graphics.Bitmap
import java.time.ZonedDateTime


typealias PhotoCategory = List<String>

data class PhotoMetadata(
    val timestamp: ZonedDateTime,
    val id: String,
    val url: String
)


data class Album(
    val id: String,
    val name: String,
    val coverPhotoUrl: String?
)

interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoList(pageSize:Int, pageToken:String?, album:Album?, category:PhotoCategory?, startDate:ZonedDateTime?, endDate:ZonedDateTime?):List<PhotoMetadata>
    suspend fun getPhotoBitmap(photo:PhotoMetadata, width:Int?, height:Int?):Bitmap?
    suspend fun getAlbumCoverPhoto(album:Album, width:Int?, height:Int?): Bitmap?
}