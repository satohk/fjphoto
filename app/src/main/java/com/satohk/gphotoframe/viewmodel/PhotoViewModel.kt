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
import kotlinx.coroutines.withContext


class PhotoViewModel(
    private val _accountState: AccountState
) : ViewModel() {
    private val _currentPhoto = MutableStateFlow<Bitmap?>(null)
    val currentPhoto: StateFlow<Bitmap?> get() = _currentPhoto
    var photoWidth: Int = 1024
    var photoHeight: Int = 768

    private var _gridContents: GridContents? = null

    private var _filteredPhotoList: FilteredPhotoList? = null
    private var _photoSelector: PhotoSelector? = null

    init{
        _accountState.photoRepository.onEach {
            if((it != null) && (_gridContents != null)){
                initPhotoSelector(it, _gridContents!!)
            }
        }.launchIn(viewModelScope)
    }

    fun onStart(gridContents: GridContents){
        if(_accountState.photoRepository.value != null){
            _gridContents = gridContents
            initPhotoSelector(_accountState.photoRepository.value!!, gridContents)
        }
    }

    fun onStop(){
        if(_photoSelector != null) {
            _photoSelector!!.stopJobs()
            _photoSelector = null
        }
    }

    private fun initPhotoSelector(repo:CachedPhotoRepository, contents:GridContents){
        _filteredPhotoList = FilteredPhotoList(repo, contents.searchQuery)
        _photoSelector = PhotoSelector(_filteredPhotoList!!)

        _photoSelector!!.currentPhotoMetadata.onEach {
            if(it != null) {
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
    }
}
