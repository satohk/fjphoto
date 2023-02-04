package com.satohk.fjphoto.repository.localrepository

import android.util.Log
import com.satohk.fjphoto.repository.data.PhotoMetadataLocal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PhotoMetadataLocalRepository(private val db: AppDatabase) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    init{
        Log.d("PhotoMetadataStore", "init")
    }

    suspend fun get(photoId: String): PhotoMetadataLocal {
        val res = withContext(ioDispatcher) {
            db.photoMetadataDao().findById(photoId)
        }
        return if (res.isNotEmpty()) {
            PhotoMetadataLocal(res.first().id, res.first().favorite)
        } else {
            defaultPhotoMetadata
        }
    }

    suspend fun getAll(accountId: String): List<PhotoMetadataLocal> {
        return withContext(ioDispatcher) {
            db.photoMetadataDao().findAll(accountId).map{it -> PhotoMetadataLocal(it.id, it.favorite)}
        }
    }

    suspend fun set(photoId: String, accountId: String, value: PhotoMetadataLocal){
        withContext(ioDispatcher) {
            if (value.favorite == defaultPhotoMetadata.favorite) {
                db.photoMetadataDao().delete(photoId)
            } else {
                db.photoMetadataDao().save(PhotoMetadataEntity(photoId, accountId, value.favorite))
            }
        }
    }

    companion object {
        val defaultPhotoMetadata = PhotoMetadataLocal(id="", favorite = false)
    }
}