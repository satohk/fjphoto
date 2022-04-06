package com.satohk.gphotoframe.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
            val url = "https://photoslibrary.googleapis.com/v1/albums?pageSize=%d%s".format(pageSize, pageTokenStr)
            val res = httpGet(url)
            if(!res.isSuccessful){
                Log.i("http", "response is not ok . %s, %s".format(url, res.toString()))
                break
            }
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

        val response = httpPost(url, requestBody)
        if(!response.isSuccessful){
            Log.i("http", "response is not ok . %s, %s".format(url, response.toString()))
            return listOf()
        }
        val responseBodyStr = response.body?.string()!!
        val responseDecoded = jsonDec.decodeFromString<MediaItemsResponse>(responseBodyStr)
        val result: List<PhotoMetadata> = responseDecoded.mediaItems.map{
            PhotoMetadata(
                ZonedDateTime.parse(it.mediaMetadata!!.creationTime),
                it.id,
                it.baseUrl
            )
        }
        return result
    }

    override suspend fun getPhotoBitmap(photo:PhotoMetadata, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        val res = httpGet(makeImageUrl(photo.url, width, height, cropFlag))
        if(!res.isSuccessful){
            Log.i("http", "response is not ok . %s, %s".format(photo.url, res.toString()))
            return null
        }
        val body = res.body?.bytes()!!
        val bmp = BitmapFactory.decodeByteArray(body, 0, body.size)
        return bmp
    }

    override suspend fun getAlbumCoverPhoto(album:Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        if(album.coverPhotoUrl != null){
            val res = httpGet(makeImageUrl(album.coverPhotoUrl, width, height, cropFlag))
            if(!res.isSuccessful){
                Log.i("http", "response is not ok . %s, %s".format(album.coverPhotoUrl, res.toString()))
                return null
            }
            val body = res.body?.bytes()!!
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

    private fun httpGet(url: String): Response {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        return client.newCall(request).execute()
    }

    private fun httpPost(url: String, requestBody: String): Response {
        val client = OkHttpClient()
        val postBody =
            requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request =
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(postBody)
                .build()
        return client.newCall(request).execute()
    }
}