package com.satohk.fjphoto.domain

import android.graphics.Bitmap
import android.util.Log
import com.satohk.fjphoto.repository.data.PhotoMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

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

    enum class MediaType{BLANK, PHOTO, VIDEO}
    class MediaMetadata(
        val metadata: PhotoMetadata? = null,
        val fadeinDuration: Int
    ){
        val index: Int = nextMediaIndex()
        val mediaType:MediaType
            get() = if(metadata == null){
                MediaType.BLANK
            }
            else if(metadata.metadataRemote.mimeType.startsWith("image")){
                MediaType.PHOTO
            }
            else if(metadata.metadataRemote.mimeType.startsWith("video")){
                MediaType.VIDEO
            }
            else{
                MediaType.BLANK
            }

        private fun nextMediaIndex(): Int{
            maxMediaIndex = (maxMediaIndex + 1) % 1024
            return maxMediaIndex
        }

        companion object{
            private var maxMediaIndex: Int = 0
        }
    }

    private val _currentMedia = MutableStateFlow(MediaMetadata(fadeinDuration = 0))
    val currentMedia: StateFlow<MediaMetadata> get() = _currentMedia
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading
    private var _stopped: Boolean = false
    private var _currentPoint: Int? = null
    private val _mediaLoadingMutex = Mutex()

    private fun getFadeInDuration(): Int{
        return if(_slideShowMode) 1000 else 100
    }

    private suspend fun calcNextPointIndex(currentPointIndex: Int?, step: Int): Int{
        var nextPointIndex = if(step == 0 && currentPointIndex != null){
            currentPointIndex
        } else if (_selectMode == SelectMode.RANDOM) {
            // If the randomly selected number is the same as last time, increment the number
            val i = (Math.random() * _photoList.size).toInt()
            if(i == currentPointIndex) i + 1 else i  //
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

    private suspend fun nextStep(step: Int, waitMillisec: Long) {
        if(!_mediaLoadingMutex.isLocked) {
            _mediaLoadingMutex.lock()
            _isLoading.value = true
            val nextPoint = calcNextPointIndex(_currentPoint, step)
            val nextMetadata = try {
                _photoList[nextPoint]
            } catch (e: java.lang.IndexOutOfBoundsException) {
                // Depending on the fragment switching timing, _photoList may have been cleared
                // so IndexOutOfBoundsException may occur.
                Log.d("PhotoSelector", "java.lang.IndexOutOfBoundsException occured on _photoList")
                null
            }
            Log.d(
                "PhotoSelector",
                "nextStep _currentPoint=$_currentPoint, nextPoint=$nextPoint, nextMetadata=$nextMetadata"
            )
            val nextMedia = MediaMetadata(nextMetadata, fadeinDuration = getFadeInDuration())
            if ((nextMedia.mediaType == MediaType.VIDEO) &&
                (_currentMedia.value.mediaType == MediaType.VIDEO)
            ) {
                // Insert a blank screen when transitioning from video to video
                _currentMedia.value = MediaMetadata(fadeinDuration = getFadeInDuration())
            } else {
                _currentMedia.value = nextMedia
            }
            _currentPoint = nextPoint
        }
    }

    suspend fun goNext(waitMillisec:Long = 0){
        if(!_slideShowMode){
            goNextSub(waitMillisec)
        }
    }

    suspend fun startSlideshow(){
        goNextSub(0)
    }

    suspend fun goNextSub(waitMillisec:Long = 0){
        Log.d("PhotoSelector", "goNext waitMillisec=$waitMillisec")
        if(!_stopped) {
            delay(waitMillisec)
        }
        if(!_stopped) {
            nextStep(1, waitMillisec)
        }
    }

    suspend fun goPrev(waitMillisec:Long = 0){
        Log.d("PhotoSelector", "goPrev waitMillisec=$waitMillisec")
        if(!_stopped) {
            delay(waitMillisec)
        }
        if(!_stopped) {
            nextStep(-1, waitMillisec)
        }
    }

    fun stop(){
        Log.d("PhotoSelector", "stop")
        _stopped = true
    }

    suspend fun loadAllMetadata(){
        Log.d("PhotoSelector", "loadAllMetadata started")
        while(!_photoList.allLoaded && !_stopped){
            _photoList.loadNext(100, false)
            delay(500)
        }
        Log.d("PhotoSelector", "loadAllMetadata ended _filteredPhotoList.size=${_photoList.size}")
    }

    suspend fun onMediaStarted(mediaIndex: Int){
        Log.d("PhotoSelector", "onMediaStarted mediaIndex=$mediaIndex currentMediaIndex=${_currentMedia.value.index}")
        if(mediaIndex != _currentMedia.value.index){
            return
        }
        _mediaLoadingMutex.unlock()
        _isLoading.value = false
        if(_currentMedia.value.mediaType == MediaType.BLANK){
            // Blank screen is inserted when transitioning from video to video
            nextStep(0, 0)
        }
        else if(_slideShowMode){
            if((_currentMedia.value.mediaType == MediaType.VIDEO && _slideShowCutPlay) ||
                (_currentMedia.value.mediaType == MediaType.PHOTO)
            ){
                goNextSub(_slideShowIntervalMillisec)
            }
        }
    }

    suspend fun onMediaEnded(mediaIndex: Int){
        Log.d("PhotoSelector", "onMediaEnded mediaIndex=$mediaIndex currentMediaIndex=${_currentMedia.value.index}")
        if(mediaIndex != _currentMedia.value.index){
            return
        }
        if(_slideShowMode && !_slideShowCutPlay) {
            goNextSub(0)
        }
    }

    suspend fun onMediaError(mediaIndex: Int){
        Log.d("PhotoSelector", "onMediaEnded mediaIndex=$mediaIndex currentMediaIndex=${_currentMedia.value.index}")
        if(mediaIndex != _currentMedia.value.index){
            return
        }
        if(_slideShowMode) {
            goNextSub(0)
        }
    }
}