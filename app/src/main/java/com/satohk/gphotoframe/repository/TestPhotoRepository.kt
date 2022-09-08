package com.satohk.gphotoframe.repository

import android.accounts.NetworkErrorException
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.model.*
import com.satohk.gphotoframe.model.MediaItemsResponse
import com.satohk.gphotoframe.model.MediaType
import com.satohk.gphotoframe.model.ParamContentCategory
import com.satohk.gphotoframe.model.ParamContentFilter
import com.satohk.gphotoframe.model.ParamDate
import com.satohk.gphotoframe.model.ParamDateFilter
import com.satohk.gphotoframe.model.ParamDateRange
import com.satohk.gphotoframe.model.ParamFilters
import com.satohk.gphotoframe.model.ParamMediaType
import com.satohk.gphotoframe.model.ParamMediaTypeFilter
import com.satohk.gphotoframe.model.SearchParam
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import okhttp3.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.ConnectException


class TestPhotoRepository(
    private val accessToken:String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO)
    : GooglePhotoRepository(accessToken, ioDispatcher){

    var samplePhotoMetadataList: List<PhotoMetadata>? = null
    var sampleBitmap: Bitmap? = null
    var count: Int = 0

    override suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryForRepo?):Pair<List<PhotoMetadata>,String?>{
        //return super.getNextPhotoMetadataList(pageSize, pageToken, searchQuery)
        if(samplePhotoMetadataList == null){
            val res = super.getNextPhotoMetadataList(pageSize, pageToken, searchQuery)
            samplePhotoMetadataList = res.first
            return res
        }
        else{
            val newList = samplePhotoMetadataList!!.map{PhotoMetadata(it.timestamp, it.id+count.toString(), it.url)}
            count += 1
            return Pair(newList, "nextToken")
        }
    }

    override suspend fun getPhotoBitmap(
        photo: PhotoMetadata,
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