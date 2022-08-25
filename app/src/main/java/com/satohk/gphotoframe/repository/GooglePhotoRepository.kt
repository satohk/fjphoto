package com.satohk.gphotoframe.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.satohk.gphotoframe.model.*
import com.satohk.gphotoframe.model.AlbumsResponse
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


class GooglePhotoRepository(
    private val accessToken:String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : PhotoRepository {
    private val jsonDec = Json{ignoreUnknownKeys=true}

    override suspend fun getAlbumList():List<Album> {
        val pageSize = 50
        var pageToken:String? = null

        val albums: MutableList<Album> = mutableListOf()
        var exitLoop = false

        while(!exitLoop){
            val pageTokenStr = if (pageToken == null)  "" else "&pageToken=%s".format(pageToken)
            val url = "https://photoslibrary.googleapis.com/v1/albums?pageSize=%d%s".format(pageSize, pageTokenStr)
            val res = httpGet(url)

            if(res.isSuccessful){
                val resBody = jsonDec.decodeFromString<AlbumsResponse>(res.body?.string()!!)
                for(album in resBody.albums){
                    albums.add(
                        Album(album.id, album.title, album.coverPhotoBaseUrl)
                    )
                }
                if(resBody.nextPageToken == null){
                    exitLoop = true
                }
                else {
                    pageToken = resBody.nextPageToken
                }
            }
            else{
                exitLoop = true
            }

            res.body?.close()
            res.close()
        }

        return albums
    }

    override suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQuery?):Pair<List<PhotoMetadata>,String?>{
        val dateFilter =
            if(searchQuery?.startDate !== null && searchQuery?.endDate !== null)
                ParamDateFilter(
                    ranges=listOf(
                        ParamDateRange(
                        startDate= ParamDate(searchQuery.startDate),
                        endDate= ParamDate(searchQuery.endDate)
                    )
                    ))
            else null
        val contentFilter =
            if(searchQuery?.photoCategory !== null)
                ParamContentFilter(includedContentCategories=searchQuery.photoCategory.map{v -> ParamContentCategory.valueOf(v)})
            else null
        val mediaTypeFilter: ParamMediaTypeFilter? =
            when(searchQuery?.mediaType){
                MediaType.ALL -> ParamMediaTypeFilter(ParamMediaType.ALL_MEDIA)
                MediaType.PHOTO -> ParamMediaTypeFilter(ParamMediaType.PHOTO)
                MediaType.VIDEO -> ParamMediaTypeFilter(ParamMediaType.VIDEO)
                else -> null
            }
        // albumと他のフィルタを同時に指定することはできない
        val filters =
            if(searchQuery?.album == null && (dateFilter !== null || contentFilter !== null || mediaTypeFilter !== null))
                ParamFilters(dateFilter=dateFilter, contentFilter=contentFilter, mediaTypeFilter=mediaTypeFilter)
            else null
        val searchParam = SearchParam(
            albumId=searchQuery?.album?.id,
            pageSize=pageSize,
            pageToken=pageToken,
            filters=filters
        )

        val format = Json { encodeDefaults = false }
        val requestBody = format.encodeToString(searchParam)
        val url = "https://photoslibrary.googleapis.com/v1/mediaItems:search"

        val response = httpPost(url, requestBody)
        if(!response.isSuccessful){
            Log.i("http", "response is not ok . %s, %s".format(url, response.toString()))
            response.body?.close()
            response.close()
            return Pair(listOf(), "")
        }
        val responseBodyStr = response.body?.string()!!
        val responseDecoded = jsonDec.decodeFromString<MediaItemsResponse>(responseBodyStr)
        val resultNextPageToken = responseDecoded.nextPageToken
        val result: List<PhotoMetadata> = responseDecoded.mediaItems?.map{
            PhotoMetadata(
                ZonedDateTime.parse(it.mediaMetadata!!.creationTime),
                it.id,
                it.baseUrl
            )
        } ?: listOf()
        response.body?.close()
        response.close()
        return Pair(result, resultNextPageToken)
    }

    override suspend fun getPhotoBitmap(
        photo: PhotoMetadata,
        width: Int?,
        height: Int?,
        cropFlag: Boolean?
    ): Bitmap? {
        return withContext(ioDispatcher) {
            Log.d("getPhotoBitmap", "w%d, h%d".format(width, height))
            try {
                val res = httpGet(makeImageUrl(photo.url, width, height, cropFlag))
                if (!res.isSuccessful) {
                    Log.i("http", "response is not ok . %s, %s".format(photo.url, res.toString()))
                    res.close()
                    res.body?.close()
                    return@withContext null
                }

                val body = res.body?.bytes()!!
                val bmp = BitmapFactory.decodeByteArray(body, 0, body.size)
                res.close()
                res.body?.close()
                return@withContext bmp            }
            catch(e: ConnectException){
                Log.e("getPhotoBitmap", e.toString())
                return@withContext null
            }
        }
    }

    override suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        if(album.coverPhotoUrl != null){
            val res = httpGet(makeImageUrl(album.coverPhotoUrl, width, height, cropFlag))
            if(!res.isSuccessful){
                Log.i("http", "response is not ok . %s, %s".format(album.coverPhotoUrl, res.toString()))
                res.body?.close()
                res.close()
                return null
            }
            val body = res.body?.bytes()!!
            res.body?.close()
            res.close()
            val bmp = BitmapFactory.decodeByteArray(body, 0, body.size)
            return bmp
        }
        else{
            return null
        }
    }

    private fun makeImageUrl(baseUrl:String, width:Int?, height:Int?, cropFlag:Boolean?):String{
        var url = baseUrl
        if(width != null){
            url += "=w%d-h%d".format(width, height)
        }
        if(cropFlag != null && cropFlag == true){
            url += "-c"
        }
        return url
    }

    private suspend fun httpGet(url: String): Response {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        var response: Response? = null
        withContext(ioDispatcher) {
            response = client.newCall(request).execute()
        }
        return response!!
    }

    private suspend fun httpPost(url: String, requestBody: String): Response {
        Log.d("httpPost", "$url/$requestBody")
        val client = OkHttpClient()
        val postBody =
            requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request =
            Request.Builder().url(url)
                                .addHeader("Authorization", "Bearer $accessToken")
                                .post(postBody)
                                .build()
        var response: Response? = null
        withContext(ioDispatcher) {
            response = client.newCall(request).execute()
        }
        return response!!
    }

    override fun getCategoryList(): List<String>{
        return ParamContentCategory.values().map{ v -> v.toString() }
    }
}