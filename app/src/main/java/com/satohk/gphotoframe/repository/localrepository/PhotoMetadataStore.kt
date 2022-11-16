package com.satohk.gphotoframe.repository.localrepository

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import com.satohk.gphotoframe.repository.data.PhotoMetadataLocal


class PhotoMetadataStore(private val _fileNamePrefix:String) {
    private val _fileName = "photoMetadata.json"
    private var _photoMetadataLocalMap = mutableMapOf<String, PhotoMetadataLocal>()

    init{
        Log.d("PhotoMetadataStore.init", _fileNamePrefix)
        loadFromLocalFile()
    }

    operator fun get(key: String): PhotoMetadataLocal {
        return if(_photoMetadataLocalMap.containsKey(key)){
            _photoMetadataLocalMap[key]!!
        } else{
            defaultPhotoMetadat
        }
    }

    operator fun set(key: String, value: PhotoMetadataLocal){
        if(value == defaultPhotoMetadat){
            _photoMetadataLocalMap.remove(key)
        }
        else{
            _photoMetadataLocalMap[key] = value
        }
    }

    fun saveToLocalFile(){
        val content = Json.encodeToString(this._photoMetadataLocalMap)
        val file = File(filesDir, _fileNamePrefix + _fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }
        Log.d("saveToLocalFile", content)
        Log.d("saveToLocalFile", filesDir.toString())
    }

    fun loadFromLocalFile(){
        val file = File(filesDir, _fileNamePrefix + _fileName)
        if(file.isFile){
            FileReader(file).use { reader ->
                val content = reader.readText()
                _photoMetadataLocalMap = Json.decodeFromString<MutableMap<String, PhotoMetadataLocal>>(content)
                Log.d("loadFromLocalFile", _photoMetadataLocalMap.toString())
            }
        }
        else{
            Log.d("loadFromLocalFile", "file is not exist")
        }
    }

    companion object {
        val defaultPhotoMetadat = PhotoMetadataLocal(favorite = false)
        var filesDir: String = ""
    }
}