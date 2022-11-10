package com.satohk.gphotoframe.repository.remoterepository

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.satohk.gphotoframe.repository.entity.Album
import com.satohk.gphotoframe.repository.entity.MediaType
import com.satohk.gphotoframe.repository.entity.SearchQueryRemote
import com.satohk.gphotoframe.repository.entity.PhotoMetadataRemote


class TestPhotoRepository(
    private val accessToken:String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO)
    : GooglePhotoRepository(accessToken, ioDispatcher){

    var samplePhotoMetadataList: List<PhotoMetadataRemote>? = null
    var sampleBitmap: Bitmap? = null
    var count: Int = 0

    override suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryRemote?):Pair<List<PhotoMetadataRemote>,String?>{
        //return super.getNextPhotoMetadataList(pageSize, pageToken, searchQuery)
        if(samplePhotoMetadataList == null){
            val res = super.getNextPhotoMetadataList(pageSize, pageToken, searchQuery)
            samplePhotoMetadataList = res.first
            return res
        }
        else{
            val newList = samplePhotoMetadataList!!.map{ PhotoMetadataRemote(it.timestamp, it.id+count.toString(), it.url, it.productUrl, it.mimeType) }
            count += 1
            return Pair(newList, "nextToken")
        }
    }

    override suspend fun getPhotoBitmap(
        photo: PhotoMetadataRemote,
        width: Int?,
        height: Int?,
        cropFlag: Boolean?
    ): Bitmap? {
        //return super.getPhotoBitmap(photo, width, height, cropFlag)
        if(sampleBitmap == null){
            sampleBitmap = super.getPhotoBitmap(photo, width, height, cropFlag)
        }
        return sampleBitmap?.copy(sampleBitmap?.config, true)
    }

    override suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        return super.getAlbumCoverPhoto(album, width, height, cropFlag)
    }

    override fun getCategoryList(): List<String>{
        return ParamContentCategory.values().map{ v -> v.toString() }
    }
}