package com.satohk.fjphoto.repository.localrepository

import android.util.Log
import com.satohk.fjphoto.repository.data.MediaType
import com.satohk.fjphoto.repository.data.PhotoMetadataRemote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime


class PhotoMetadataRemoteCacheRepository(private val db: AppDatabase) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    init{
        Log.d("PhotoMetadataRemoteCacheRepository", "init")
    }

    private fun date2long(date: ZonedDateTime): Long{
        return date.nano / 1000000 + date.toEpochSecond() * 1000
    }

    private fun long2date(value: Long): ZonedDateTime{
        val i = Instant.ofEpochSecond(value / 1000)
        return ZonedDateTime.ofInstant(i, ZoneOffset.UTC)
    }

    private fun mimeType2MediaType(mimeType:String): String{
        return if(mimeType.startsWith("image")){
            "image"
        }
        else if(mimeType.startsWith("video")){
            "video"
        }
        else{
            ""
        }
    }

    suspend fun get(accountId: String, startDate: ZonedDateTime?, endDate: ZonedDateTime?, mediaType: MediaType?, limit: Int, offset: Int=0, desc: Boolean=false): List<PhotoMetadataRemote> {
        val startDateLong = if(startDate!=null) date2long(startDate) else 0
        val endDateLong = if(endDate!=null) date2long(endDate) else Long.MAX_VALUE
        val mimeTypes = when(mediaType) {
            MediaType.PHOTO -> listOf("image")
            MediaType.VIDEO -> listOf("video")
            else -> listOf("image", "video")
        }
        val res = withContext(ioDispatcher) {
            if (desc) {
                db.photoMetadataRemoteCacheDao()
                    .findDesc(accountId, startDateLong, endDateLong, mimeTypes, limit, offset)
            } else {
                db.photoMetadataRemoteCacheDao()
                    .findAsc(accountId, startDateLong, endDateLong, mimeTypes, limit, offset)
            }
        }
        val res2 = res.map{
            PhotoMetadataRemote(long2date(it.timestamp), it.id, null, null, it.mimeType)
        }
        return res2
    }

    suspend fun getLast(accountId: String): PhotoMetadataRemote? {
        val res = this.get(accountId, null, null, MediaType.ALL, 1, 0, true)
        return if(res.isNotEmpty()) res[0] else null
    }

    suspend fun add(accountId: String, value: PhotoMetadataRemote){
        withContext(ioDispatcher) {
            val millisec = date2long(value.timestamp)
            val cacheEntry = PhotoMetadataRemoteCacheEntity(
                value.id, millisec,
                accountId, mimeType2MediaType(value.mimeType)
            )
            db.photoMetadataRemoteCacheDao().save(cacheEntry)
        }
    }
}