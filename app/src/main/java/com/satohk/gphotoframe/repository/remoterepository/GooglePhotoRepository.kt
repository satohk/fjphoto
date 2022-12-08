package com.satohk.gphotoframe.repository.remoterepository

import android.accounts.NetworkErrorException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import com.satohk.gphotoframe.repository.data.Album
import com.satohk.gphotoframe.repository.data.MediaType
import com.satohk.gphotoframe.repository.data.SearchQueryRemote
import com.satohk.gphotoframe.repository.data.PhotoMetadataRemote


open class GooglePhotoRepository(
    private val accessToken:String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : PhotoRepository {
    private val jsonDec = Json{ignoreUnknownKeys=true}
    private val client = OkHttpClient()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getAlbumList():List<Album> {
        val pageSize = 50
        var pageToken:String? = null

        val albums: MutableList<Album> = mutableListOf()
        var exitLoop = false

        while(!exitLoop){
            val pageTokenStr = if (pageToken == null)  "" else "&pageToken=%s".format(pageToken)
            val url = "https://photoslibrary.googleapis.com/v1/albums?pageSize=%d%s".format(pageSize, pageTokenStr)
            val res = httpGet(url)

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

            res.body?.close()
            res.close()
        }

        return albums
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getNextPhotoMetadataList(pageSize:Int, pageToken:String?, searchQuery: SearchQueryRemote?)
            : Pair<List<PhotoMetadataRemote>,String?>{
        val dateFilter =
            if(searchQuery?.startDate != null && searchQuery.endDate != null)
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
        val responseBodyStr = response.body?.string()!!
        val responseDecoded = jsonDec.decodeFromString<MediaItemsResponse>(responseBodyStr)
        val resultNextPageToken = responseDecoded.nextPageToken
        Log.d("getNextPhotoMetadataList", responseDecoded.mediaItems?.get(0).toString())
        val result: List<PhotoMetadataRemote> = responseDecoded.mediaItems?.map{
            PhotoMetadataRemote(
                ZonedDateTime.parse(it.mediaMetadata!!.creationTime),
                it.id,
                it.baseUrl,
                it.productUrl,
                it.mimeType
            )
        } ?: listOf()
        response.body?.close()
        response.close()
        return Pair(result, resultNextPageToken)
    }

    override suspend fun getPhotoBitmap(
        photo: PhotoMetadataRemote,
        width: Int?,
        height: Int?,
        cropFlag: Boolean?
    ): Bitmap? {
        return try {
            val res = httpGet(makeImageUrl(photo.url, width, height, cropFlag))
            val body = res.body?.bytes()!!
            val bmp = BitmapFactory.decodeByteArray(body, 0, body.size)
            res.body?.close()
            res.close()
            bmp
        } catch(e: ConnectException){
            Log.e("getPhotoBitmap", e.toString())
            null
        }
    }

    override suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        if(album.coverPhotoUrl != null) {
            val res = httpGet(makeImageUrl(album.coverPhotoUrl, width, height, cropFlag))
            val body = res.body?.bytes()!!
            res.body?.close()
            res.close()
            return BitmapFactory.decodeByteArray(body, 0, body.size)
        }
        else{
            return null
        }
    }

    override fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRemote): Pair<PhotoRequestHeader, String>{
        val headers: MutableMap<String, String> = HashMap()
        headers["Authorization"] = "Bearer $accessToken"
        headers["Accept-Ranges"] = "bytes";
        headers["Status"] = "206";
        headers["Cache-control"] = "no-cache";

        return Pair(headers, media.url + "=dv")
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
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val response: Response = withContext(ioDispatcher) {
            client.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            Log.i(
                "http",
                "response is not ok . $url $response"
            )
            response.body?.close()
            response.close()
            throw NetworkErrorException(response.message)
        }
        return response
    }

    private suspend fun httpPost(url: String, requestBody: String): Response {
        Log.d("httpPost", "$url/$requestBody")
        val postBody =
            requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request =
            Request.Builder().url(url)
                                .addHeader("Authorization", "Bearer $accessToken")
                                .post(postBody)
                                .build()
        val response: Response = withContext(ioDispatcher) {
            client.newCall(request).execute()
        }
        if (!response.isSuccessful) {
            Log.i(
                "http",
                "response is not ok . $url $response"
            )
            response.body?.close()
            response.close()
            throw NetworkErrorException(response.message)
        }
        return response
    }

    override fun getCategoryList(): List<String>{
        return ParamContentCategory.values().map{ v -> v.toString() }
    }
}