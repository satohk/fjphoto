package com.satohk.gphotoframe.model

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PhotoSelector(
    private val _photoList: FilteredPhotoList,
    private val _selectMode: SelectMode = SelectMode.SEQUENTIAL,
    private val _slideshowWaitMilliSecond: Long = 10000,
    _startSlideshow: Boolean = true,
){
    private val _scope = CoroutineScope(Job() + Dispatchers.IO)
    private var _stopJobs: Boolean = false

    init{
        _scope.launch {
            startBulkPhotoLoad()
        }
        if(_startSlideshow) {
            _scope.launch {
                startSlideshow()
            }
        }
    }

    enum class SelectMode{
        RANDOM,
        SEQUENTIAL
    }

    private var _currentPoint: Int? = if(_photoList.list.isEmpty()) null else 0
        set(value) {
            field = value
            if(value != null) {
                _currentPhotoMetadata.value = _photoList.list[value!!]
            }
            else{
                //_currentPhotoMetadata.value = null
            }
        }

    private val _currentPhotoMetadata = MutableStateFlow<PhotoMetadata?>(null)
    val currentPhotoMetadata: StateFlow<PhotoMetadata?> get() = _currentPhotoMetadata

    private suspend fun startBulkPhotoLoad(){
        val bulkLoadCount = 10
        val loadIntervalMilliSec:Long = 500
        while(!_stopJobs) {
            val currentSize = _photoList.list.size
            _photoList.getFilteredPhotoMetadataList(currentSize, bulkLoadCount)
            Log.d("startBulkPhotoLoad", "currentSize=$currentSize, _photoList.list.size=${_photoList.list.size}")
            if(_photoList.allLoaded){
                break
            }
            delay(loadIntervalMilliSec)
        }
    }

    private suspend fun startSlideshow(){
        while(!_stopJobs){
            next()
            val delayMilliSec =
                if(_currentPhotoMetadata.value == null)
                    100
                else
                    _slideshowWaitMilliSecond
            Log.d("slideshow delay millisec", delayMilliSec.toString())
            delay(delayMilliSec)
        }
    }

    fun stopJobs(){
        _stopJobs = true
    }

    fun next() {
        if(_photoList.list.isEmpty()){
            _currentPoint = null
        }
        else if(_selectMode == SelectMode.RANDOM){
            _currentPoint = (Math.random() * _photoList.list.size).toInt()
        }
        else{ // sequential
            if(_currentPoint == null){
                _currentPoint = 0
            }
            else if(_currentPoint!! + 1 >= _photoList.list.size){
                _currentPoint = 0
            }
            else{
                _currentPoint = _currentPoint!! + 1
            }
        }
    }

    fun prev(){
        if(_photoList.list.isEmpty()){
            _currentPoint = null
        }
        else if(_selectMode == SelectMode.RANDOM){
            _currentPoint = (Math.random() * _photoList.list.size).toInt()
        }
        else{ // sequential
            if(_currentPoint == null){
                _currentPoint = 0
            }
            else if(_currentPoint!! == 0){
                _currentPoint = _photoList.list.size - 1
            }
            else{
                _currentPoint = _currentPoint!! - 1
            }
        }
    }
}