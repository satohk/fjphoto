package com.satohk.fjphoto.domain

import android.graphics.Bitmap
import android.util.Log
import com.satohk.fjphoto.repository.data.PhotoMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class PhotoSelector(
    private val _photoList: FilteredPhotoList,
    private val _selectMode: SelectMode,
    private val _initIndex: Int,
    private val _slideShowMode: Boolean,
    private val _slideShowCutPlay: Boolean,
    private val _slideShowIntervalMillisec: Long
){
    private val _bulkLoadCount = 10

    enum class SelectMode(val index:Int){
        SEQUENTIAL(0),
        RANDOM(1)
    }

    data class Media (
        var bitmap: Bitmap? = null,
        var videoUrl: String? = null,
        val photoMetadata: PhotoMetadata? = null,
        val viewIndex: Int = 0,
        var state: State = State.BEFORE_LOAD,
    ){
        val mediaId: Int = getNextMediaId()

        enum class State{ BEFORE_LOAD, LOADING, LOADED, FADE_IN, SHOWING, ENDED, ERROR }

        private fun getNextMediaId(): Int{
            nextMediaId = (nextMediaId + 1) % 1024
            return nextMediaId
        }

        companion object{
            var nextMediaId = 0
        }
    }

    private val _prepareMedia = MutableSharedFlow<Media?>(1)
    val prepareMedia: SharedFlow<Media?> get() = _prepareMedia
    private val _showMedia = MutableSharedFlow<Media?>(1)
    val showMedia: SharedFlow<Media?> get() = _showMedia
    private val _preparingCurrentMedia = MutableStateFlow(false)
    val preparingCurrentMedia: StateFlow<Boolean> get() = _preparingCurrentMedia
    private var _stopped: Boolean = false

    private var _currentPoint: Int? = null
    private var _nextPoint: Int? = null
    private val _mediaBuf = mutableListOf(Media(), Media())
    private val _mediaBufWriteMutex = Mutex()
    private val _preparingCurrentMediaMutex = Mutex()

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

        // load if nextPint exceed photoList size
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

    private suspend fun decideNextPhotoMetadata(step: Int, waitMillisec: Long): Pair<PhotoMetadata?, PhotoMetadata?>{
        val beginTime = System.nanoTime()

        if (step == 1 && _nextPoint != null) {
            _currentPoint = _nextPoint
            _nextPoint = calcNextPointIndex(_currentPoint, step)
        } else {
            _currentPoint = calcNextPointIndex(_currentPoint, step)
            _nextPoint = calcNextPointIndex(_currentPoint, 1)
        }

        var currentMetadata: PhotoMetadata? = null
        var nextMetadata: PhotoMetadata? = null
        try{
            currentMetadata = _photoList[_currentPoint!!]
            nextMetadata = _photoList[_nextPoint!!]
        }
        catch(e: java.lang.IndexOutOfBoundsException){
            // Depending on the fragment switching timing, _photoList may have been cleared
            // so IndexOutOfBoundsException may occur.
            Log.d("PhotoSelector", "java.lang.IndexOutOfBoundsException occured on _photoList")
            currentMetadata = null
            nextMetadata = null
        }

        val elapsedTimeMilliSec = ((System.nanoTime() - beginTime) / 1000000)
        delay(max(waitMillisec - elapsedTimeMilliSec, 0))

        return Pair(currentMetadata, nextMetadata)
    }

    private suspend fun nextStep(step: Int, waitMillisec: Long) {
        Log.d("PhotoSelector", "nextStep step=$step, waitMillisec=$waitMillisec")
        if(!_preparingCurrentMediaMutex.tryLock()){
            return
        }

        val res = decideNextPhotoMetadata(step, waitMillisec)
        val current = res.first
        val next = res.second

         _mediaBufWriteMutex.withLock {
            if(_mediaBuf[1].photoMetadata?.metadataRemote?.id == current?.metadataRemote?.id){
                _mediaBuf[0] = _mediaBuf[1]
            }
            else{
                _mediaBuf[0] = Media(null, null, current,
                    _mediaBuf[1].viewIndex, Media.State.BEFORE_LOAD)
            }
            _mediaBuf[1] = Media(null, null, next,
                (_mediaBuf[1].viewIndex + 1) % 2,
                Media.State.BEFORE_LOAD
            )
        }
        onMediaStateChanged()
    }

    suspend fun onMediaStateChanged(){
        Log.d("PhotoSelector", "onMediaStateChanged mediaId=${_mediaBuf[0].mediaId}, state=${_mediaBuf[0].state}")
        Log.d("PhotoSelector", "onMediaStateChanged next mediaId=${_mediaBuf[1].mediaId}, state=${_mediaBuf[1].state}")
        if(_stopped){
            return
        }

        if(_mediaBuf[0].state == Media.State.BEFORE_LOAD || _mediaBuf[0].state == Media.State.LOADING){
            _preparingCurrentMedia.value = true
        }
        else{
            if(_preparingCurrentMediaMutex.isLocked) {
                _preparingCurrentMediaMutex.unlock()
            }
            _preparingCurrentMedia.value = false
        }

        when(_mediaBuf[0].state){
            Media.State.BEFORE_LOAD -> _prepareMedia.emit(_mediaBuf[0])
            Media.State.LOADED -> _showMedia.emit(_mediaBuf[0])
            Media.State.SHOWING -> {
                if(_mediaBuf[1].state == Media.State.BEFORE_LOAD){
                    _prepareMedia.emit(_mediaBuf[1])
                    if(_slideShowMode && (_slideShowCutPlay || (_mediaBuf[0].videoUrl == null)) ){
                        goNext(_slideShowIntervalMillisec)
                    }
                }
            }
            Media.State.ENDED -> {
                if(_slideShowMode && !_slideShowCutPlay && (_mediaBuf[0].videoUrl != null)){
                    goNext(0)
                }
            }
            Media.State.ERROR -> {
                if(_slideShowMode) {
                    _prepareMedia.emit(_mediaBuf[0])
                }
            }
            else -> {}
        }
    }
    suspend fun goNext(waitMillisec:Long = 0){
        if(!_stopped) {
            nextStep(1, waitMillisec)
        }
    }

    suspend fun goPrev(waitMillisec:Long = 0){
        if(!_stopped) {
            nextStep(-1, waitMillisec)
        }
    }

    fun stop(){
        _stopped = true
    }

    suspend fun loadAllMetadata(){
        Log.d("PhotoSelector", "loadAllMetadata started")
        while(!_photoList.allLoaded && !_stopped){
            _photoList.loadNext(100, false)
            delay(500)
            Log.d("PhotoSelector", "loadAllMetadata _filteredPhotoList.size=${_photoList.size}")
        }
        Log.d("PhotoSelector", "loadAllMetadata ended")
    }
}