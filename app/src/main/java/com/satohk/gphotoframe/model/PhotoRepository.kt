package com.satohk.gphotoframe.model

import android.graphics.Bitmap


interface PhotoRepository{
    suspend fun getAlbumList():List<Album>
    suspend fun getPhotoBitmap(photo:PhotoMetadata, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap?
    suspend fun getAlbumCoverPhoto(album:Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap?
    suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery:SearchQuery?):Pair<List<PhotoMetadata>,String?>

    fun getCategoryList(): List<String>
}