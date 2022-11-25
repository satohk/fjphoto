package com.satohk.gphotoframe.repository.localrepository

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileReader
import java.io.FileWriter
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
            PhotoMetadataLocal(res.first().favorite)
        } else {
            defaultPhotoMetadat
        }
    }

    suspend fun getAll(): List<PhotoMetadataLocal> {
        return withContext(ioDispatcher) {
            db.photoMetadataDao().findAll().map{it -> PhotoMetadataLocal(it.favorite)}
        }
    }

    suspend fun set(key: String, value: PhotoMetadataLocal){
        withContext(ioDispatcher) {
            if (value == defaultPhotoMetadat) {
                db.photoMetadataDao().delete(key)
            } else {
                db.photoMetadataDao().save(PhotoMetadataEntity(key, value.favorite))
            }
        }
    }

    companion object {
        val defaultPhotoMetadat = PhotoMetadataLocal(favorite = false)
    }
}