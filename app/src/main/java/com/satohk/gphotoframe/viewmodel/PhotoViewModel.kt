package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
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
    private val _accountState: AccountState
) : ViewModel() {
    private val _currentPhoto = MutableStateFlow<Bitmap?>(null)
    val currentPhoto: StateFlow<Bitmap?> get() = _currentPhoto
    var photoWidth: Int = 1024
    var photoHeight: Int = 768
    var currentPhotoMetadata: PhotoMetadata? = null
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
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _photoSelector!!.start(slideShow)
        }
    }
}