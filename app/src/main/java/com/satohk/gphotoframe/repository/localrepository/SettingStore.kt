package com.satohk.gphotoframe.repository.localrepository

/**
import android.util.Log
import com.satohk.gphotoframe.domain.PhotoMetadataFromLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader

class SettingStore {
    val slideshowInterval = MutableStateFlow<Int>(0)
    val columnNum = MutableStateFlow<Int>(6)

    fun loadFromLocalFile(){
        val file = File(PhotoMetadataStore.filesDir, _fileNamePrefix + _fileName)
        if(file.isFile){
            FileReader(file).use { reader ->
                val content = reader.readText()
                _photoMetadataLocalMap = Json.decodeFromString<MutableMap<String, PhotoMetadataFromLocal>>(content)
                Log.d("loadFromLocalFile", _photoMetadataLocalMap.toString())
            }
        }
        else{
            Log.d("loadFromLocalFile", "file is not exist")
        }
    }
}
        */