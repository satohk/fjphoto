package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.*
import com.satohk.gphotoframe.repository.CachedPhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PhotoViewModel(
    val _accountState: AccountState
) : ViewModel() {

    data class VideoRequest(
        val header: Map<String, String>,
        val url: String
    )

    private val _currentPhoto = MutableStateFlow<Bitmap?>(null)
    val currentPhoto: StateFlow<Bitmap?> get() = _currentPhoto
    private val _currentVideoRequest = MutableStateFlow<VideoRequest?>(null)
    val currentVideoRequest: StateFlow<VideoRequest?> get() = _currentVideoRequest
    var photoWidth: Int = 1024
    var photoHeight: Int = 768
    var currentPhotoMetadata: PhotoMetadataRepo? = null
        private set

    private var _gridContents: GridContents? = null

    private var _filteredPhotoList: FilteredPhotoList? = null
    private var _photoSelector: PhotoSelector? = null
    private var _slideShow: Boolean = true
    private var _showIndex: Int = 0

    init{
        _accountState.photoRepository.onEach {
            if((it != null) && (_gridContents != null)){
                initPhotoSelector(it, _gridContents!!, _slideShow, _showIndex)
            }
        }.launchIn(viewModelScope)
    }

    fun onStart(gridContents: GridContents, slideShow: Boolean, showIndex: Int){
        if(_accountState.photoRepository.value != null){
            _gridContents = gridContents
            _slideShow = slideShow
            _showIndex = showIndex
            initPhotoSelector(_accountState.photoRepository.value!!, gridContents, slideShow, showIndex)
        }
    }

    fun onStop(){
        if(_photoSelector != null) {
            _photoSelector!!.stop()
            _photoSelector = null
        }
    }

    fun goNext(){
        if(_photoSelector != null) {
            viewModelScope.launch {
                _photoSelector!!.goNext()
            }
        }
    }

    fun goPrev(){
        if(_photoSelector != null) {
            viewModelScope.launch {
                _photoSelector!!.goPrev()
            }
        }
    }

    private fun initPhotoSelector(repo:CachedPhotoRepository, contents:GridContents, slideShow: Boolean, showIndex: Int){
        onStop()

        _filteredPhotoList = FilteredPhotoList(repo, contents.searchQuery)
        _photoSelector = PhotoSelector(
            _filteredPhotoList!!,
            PhotoSelector.SelectMode.SEQUENTIAL,
            10000,
            showIndex
        )

        _photoSelector!!.currentPhotoMetadata.onEach {
            if(it != null) {
                currentPhotoMetadata = it
                if(it.mimeType.startsWith("image")) {
                    withContext(Dispatchers.IO) {
                        val bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                            it!!,
                            photoWidth,
                            photoHeight,
                            false
                        )
                        _currentPhoto.value = bmp
                    }
                }
                else if(it.mimeType.startsWith("video")){
                    val tmp = _accountState.photoRepository.value!!.getMediaAccessHeaderAndUrl(currentPhotoMetadata!!)
                    _currentVideoRequest.value = VideoRequest(
                        tmp.first,
                        tmp.second
                    )
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _photoSelector!!.start(slideShow)
        }
    }
}
