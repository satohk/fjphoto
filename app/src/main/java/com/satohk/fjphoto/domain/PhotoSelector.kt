package com.satohk.fjphoto.domain

import android.util.Log
import com.satohk.fjphoto.repository.data.PhotoMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.math.max

class PhotoSelector(
    private val _photoList: FilteredPhotoList,
    private val _selectMode: SelectMode = SelectMode.SEQUENTIAL,
    private val _initIndex: Int = 0
){
    private val _bulkLoadCount = 10

    enum class SelectMode{
        RANDOM,
        SEQUENTIAL
    }

    private var _currentPoint: Int? = null
    private var _nextPoint: Int? = null

    private val _currentPhotoMetadata = MutableStateFlow<Pair<PhotoMetadata,PhotoMetadata>?>(null)
    val currentPhotoMetadata: StateFlow<Pair<PhotoMetadata,PhotoMetadata>?> get() = _currentPhotoMetadata

    private suspend fun calcNextPointIndex(currentPointIndex: Int?, step: Int): Int{
        var nextPointIndex = if (_selectMode == SelectMode.RANDOM) {
            (Math.random() * _photoList.size).toInt()
        } else if(currentPointIndex == null) {
            _initIndex
        }
        else{
            currentPointIndex + step
        }
        Log.d("calcNextPointIndex", "currentPointIndex=$currentPointIndex, nextPointIndex=$nextPointIndex")

        // load if nextPint exceed potoList size
        if (!_photoList.allLoaded){
            val exceed = nextPointIndex - _photoList.size
            if(exceed >= 0) {
                Log.d("calcNextPointIndex", "_photoList.loadNext _bulkLoadCount=$_bulkLoadCount")
                _photoList.loadNext(_bulkLoadCount, true)
            }
        }

        // fix next point
        if(nextPointIndex < 0){
            nextPointIndex = _photoList.size - 1
        }
        else if (nextPointIndex >= _photoList.size){
            nextPointIndex = 0
        }
        return nextPointIndex
    }

    private suspend fun nextStep(step: Int, waitMillisec: Long) {
        val beginTime = System.nanoTime()

        if (step == 1 && _nextPoint != null) {
            _currentPoint = _nextPoint
            _nextPoint = calcNextPointIndex(_currentPoint, step)
        } else {
            _currentPoint = calcNextPointIndex(_currentPoint, step)
            _nextPoint = calcNextPointIndex(_currentPoint, 1)
        }

        val elapsedTimeMilliSec = ((System.nanoTime() - beginTime) / 1000000)
        delay(max(waitMillisec - elapsedTimeMilliSec, 0))

        _currentPhotoMetadata.value =
            Pair(_photoList[_currentPoint!!], _photoList[_nextPoint!!])
    }

    suspend fun goNext(waitMillisec:Long = 0){
        nextStep(1, waitMillisec)
    }

    suspend fun goPrev(waitMillisec:Long = 0){
        nextStep(-1, waitMillisec)
    }
}