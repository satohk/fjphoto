package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


class PhotoGridViewModel() : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)
    private var _gridContents: GridContents? = null

    private val _itemList = mutableListOf<PhotoGridItem>()
    val itemList:List<PhotoGridItem> get(){return _itemList}

    private val _readPageSize = 50

    private var _pageToken: String? = null

    private val _dataSize = MutableStateFlow<Int>(0)
    val dataSize: StateFlow<Int> get() = _dataSize
    var lastDataSize: Int = 0
        private set

    private var _dataLoadJob: Job? = null

    fun setGridContents(gridContents: GridContents){
        if(_accountState.photoRepository.value != null && gridContents != _gridContents) {
            viewModelScope.launch {
                // stop dataload job
                _dataLoadJob?.cancel()
                _dataLoadJob?.join()
                _dataLoadJob = null

                _gridContents = gridContents
                _pageToken = null
                _itemList.clear()
                lastDataSize = dataSize.value
                _dataLoadJob = null
                _dataSize.emit(0)
                loadNextImageList()
            }
        }
    }

    fun loadNextImageList() {
        if((_accountState.photoRepository.value != null) && (_dataLoadJob == null) && (_gridContents != null)){
            _dataLoadJob = viewModelScope.launch {
                val result = _accountState.photoRepository.value!!.getPhotoList(
                    _readPageSize, _pageToken, _gridContents!!.searchQuery
                )
                val photoMetaList = result.first
                _pageToken = result.second
                _itemList.addAll(photoMetaList.map { PhotoGridItem(it) })
                lastDataSize = _dataSize.value
                _dataSize.emit(_dataSize.value + photoMetaList.size)
                _dataLoadJob = null

                Log.d("loadNextImageList", photoMetaList.size.toString())
            }
            Log.d("loadNextImageList launched", "")
        }
        else{
            Log.d("loadNextImageList", "_accountState.photoRepository.value is null")
        }
    }

    fun loadThumbnail(photoGridItem: PhotoGridItem, width:Int?, height:Int?, callback:(bmp:Bitmap?)->Unit) {
        if(_accountState.photoRepository.value != null) {
            viewModelScope.launch {
                val bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                    photoGridItem.photoMetaData,
                    width,
                    height,
                    true
                )
                callback.invoke(bmp)
            }
        }
    }

    data class PhotoGridItem(
        val photoMetaData: PhotoMetadata
    ) {
        companion object {
            val DIFF_UTIL = object: DiffUtil.ItemCallback<PhotoGridItem>() {
                override fun areItemsTheSame(oldItem: PhotoGridItem, newItem: PhotoGridItem)
                        : Boolean {
                    return oldItem.photoMetaData.url == newItem.photoMetaData.url
                }

                override fun areContentsTheSame(oldItem: PhotoGridItem, newItem: PhotoGridItem)
                        : Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}
