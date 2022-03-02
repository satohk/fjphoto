package com.satohk.gphotoframe.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.time.ZonedDateTime
import okhttp3.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


class GooglePhotoRepository(private val accessToken:String) : PhotoRepository{
    private val jsonDec = Json{ignoreUnknownKeys=true}

    override suspend fun getAlbumList():List<Album> {
        val pageSize = 50
        var pageToken:String? = null

        val albums: MutableList<Album> = mutableListOf()

        while(true){
            val pageTokenStr = if (pageToken == null)  "" else "&pageToken=%s".format(pageToken)
            val res = httpGet("https://photoslibrary.googleapis.com/v1/albums?pageSize=%d%s".format(pageSize, pageTokenStr))
            val resBody = jsonDec.decodeFromString<AlbumsResponse>(res.body?.string()!!)
            for(album in resBody.albums){
                albums.add(
                    Album(album.id, album.title, album.coverPhotoBaseUrl)
                )
            }
            if(resBody.nextPageToken == null){
                break
            }
            pageToken = resBody.nextPageToken
        }

        return albums
    }

    override suspend fun getPhotoList(pageSize:Int, pageToken:String?, album:Album?, category:PhotoCategory?, startDate:ZonedDateTime?, endDate:ZonedDateTime?):List<PhotoMetadata>{
        val dateFilter =
            if(startDate !== null && endDate !== null)
                ParamDateFilter(
                    ranges=listOf(ParamDateRange(
                        startDate=ParamDate(startDate),
                        endDate=ParamDate(endDate)
                    )))
            else null
        val conditionFilter =
            if(category !== null)
                ParamContentFilter(includedContentCategories=category)
            else null
        val filters =
            if(dateFilter !== null || conditionFilter !== null)
                ParamFilters(dateFilter=dateFilter, contentFilter=conditionFilter)
            else null
        val searchParam = SearchParam(
            albumId=album?.id,
            pageSize=pageSize,
            pageToken=pageToken,
            filters=filters
        )

        val format = Json { encodeDefaults = false }
        val requestBody = format.encodeToString(searchParam)
        val url = "https://photoslibrary.googleapis.com/v1/mediaItems:search"

        val responseBodyStr = httpPost(url, requestBody)
        val response = jsonDec.decodeFromString<MediaItemsResponse>(responseBodyStr)
        val result: List<PhotoMetadata> = response.mediaItems.map{
            PhotoMetadata(
                ZonedDateTime.parse(it.mediaMetadata!!.creationTime),
                it.id,
                it.baseUrl
            )
        }
        return result
    }

    override suspend fun getPhotoBitmap(photo:PhotoMetadata, width:Int?, height:Int?): Bitmap? {
        val res = httpGet(makeImageUrl(photo.url, width, height))
        val body = res.body?.bytes()!!
        val bmp = BitmapFactory.decodeByteArray(body, 0, body.size)
        return bmp
    }

    override suspend fun getAlbumCoverPhoto(album:Album, width:Int?, height:Int?): Bitmap? {
        if(album.coverPhotoUrl != null){
            val res = httpGet(makeImageUrl(album.coverPhotoUrl, width, height))
            val body = res.body?.bytes()!!
            val bmp = BitmapFactory.decodeByteArray(body, 0, body.size)
            return bmp
        }
        else{
            return null
        }
    }

    private fun makeImageUrl(baseUrl:String, width:Int?, height:Int?):String{
        var url = baseUrl
        if(width != null){
            url = url + "=w%d-h%d".format(width, height)
        }
        return url
    }

    private fun httpGet(url : String): Response {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        val response = client.newCall(request).execute()
        return response
    }

    private fun httpPost(url : String, requestBody: String): String {
        val client = OkHttpClient()
        val postBody =
            requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request =
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(postBody)
                .build()
        val response = client.newCall(request).execute()
        return response.body?.string()!!
    }
}