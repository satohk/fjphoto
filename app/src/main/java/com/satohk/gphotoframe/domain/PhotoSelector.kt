package com.satohk.gphotoframe.domain

import com.satohk.gphotoframe.repository.data.PhotoMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

class PhotoSelector(
    private val _photoList: FilteredPhotoList,
    private val _selectMode: SelectMode = SelectMode.SEQUENTIAL,
    private val _slideshowWaitMilliSecond: Long = 10000,
    private val _initIndex: Int = 0
){
    private var _stopJobs = false
    private val _bulkLoadCount = 10

    enum class SelectMode{
        RANDOM,
        SEQUENTIAL
    }

    private var _currentPoint: Int = _initIndex

    private val _currentPhotoMetadata = MutableStateFlow<PhotoMetadata?>(null)
    val currentPhotoMetadata: StateFlow<PhotoMetadata?> get() = _currentPhotoMetadata

    private suspend fun nextStep(step: Int, waitMillisec: Long) {
        val beginTime = System.nanoTime()

        // calc next point
        if (_selectMode == SelectMode.RANDOM) {
            _currentPoint = (Math.random() * _photoList.size).toInt()
        } else { // sequential
            _currentPoint += step
        }

        // load
        if (!_photoList.allLoaded){
            val size = max(
                _currentPoint - _photoList.size + _bulkLoadCount,
                _bulkLoadCount
            )
            _photoList.loadNext(size)
        }

        // fix currentPoint
        if(_currentPoint < 0){
            _currentPoint = _photoList.size - 1
        }
        else if (_currentPoint >= _photoList.size - 1){
            _currentPoint = 0
        }

        // wait
        val elapsedTimeMilliSec = ((System.nanoTime() - beginTime) / 1000000)
        delay(max(waitMillisec - elapsedTimeMilliSec, 0))

        // set current photo
        if(_currentPoint < _photoList.size) {
            _currentPhotoMetadata.value = _photoList[_currentPoint]
        }
    }

    suspend fun start(slideShow: Boolean){
        //show first photo
        nextStep(0, 0)

        if(slideShow) {
            while (!_stopJobs) {
                nextStep(1, _slideshowWaitMilliSecond)
            }
        }
    }

    fun stop(){
        _stopJobs = true
    }

    suspend fun goNext(){
        nextStep(1, 0)
    }

    suspend fun goPrev(){
        nextStep(-1, 0)
    }
}