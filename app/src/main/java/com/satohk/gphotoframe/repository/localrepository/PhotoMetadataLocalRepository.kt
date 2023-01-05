package com.satohk.gphotoframe.repository.localrepository

import android.util.Log
import com.satohk.gphotoframe.repository.data.PhotoMetadataLocal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PhotoMetadataLocalRepository(private val db: AppDatabase) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    init{
        Log.d("PhotoMetadataStore", "init")
    }

    suspend fun get(key: String): PhotoMetadataLocal {
        val res = withContext(ioDispatcher) {
            db.photoMetadataDao().findById(key)
        }
        return if (res.isNotEmpty()) {
            PhotoMetadataLocal(res.first().id, res.first().favorite)
        } else {
            defaultPhotoMetadata
        }
    }

    suspend fun getAll(): List<PhotoMetadataLocal> {
        return withContext(ioDispatcher) {
            db.photoMetadataDao().findAll().map{it -> PhotoMetadataLocal(it.id, it.favorite)}
        }
    }

    suspend fun set(key: String, value: PhotoMetadataLocal){
        withContext(ioDispatcher) {
            if (value.favorite == defaultPhotoMetadata.favorite) {
                db.photoMetadataDao().delete(key)
            } else {
                db.photoMetadataDao().save(PhotoMetadataEntity(key, value.favorite))
            }
        }
    }

    companion object {
        val defaultPhotoMetadata = PhotoMetadataLocal(id="", favorite = false)
    }
}