package com.satohk.gphotoframe.model

import android.graphics.Bitmap


interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoList(pageSize:Int, pageToken:String?, searchQuery:SearchQuery?):Pair<List<PhotoMetadata>,String?>
    suspend fun getPhotoBitmap(photo:PhotoMetadata, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap?
    suspend fun getAlbumCoverPhoto(album:Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap?

    fun getCategoryList(): List<String>
}