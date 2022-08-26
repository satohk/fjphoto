package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PhotoGridViewModel(
    private val _accountState: AccountState
) : SideBarActionPublisherViewModel() {
    private var _gridContents: GridContents? = null
    val gridContents: GridContents? get() = _gridContents
    private val _readPageSize = 60
    private var _dataLoadJob: Job? = null
    var lastDataSize: Int = 0
        private set

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val _itemList = mutableListOf<PhotoGridItem>()
    val itemList:List<PhotoGridItem> get(){return _itemList}
    private val _dataSize = MutableStateFlow<Int>(0)
    val dataSize: StateFlow<Int> get() = _dataSize

    init{
        _accountState.photoRepository.onEach {
            if(it !== null){
                if(_gridContents !== null){
                    val tmp:GridContents = _gridContents!!
                    _gridContents = null
                    setGridContents(tmp)
                }
            }
        }.launchIn(viewModelScope)
    }


    fun setGridContents(gridContents: GridContents){
        if(_accountState.photoRepository.value != null) {
            if(gridContents == _gridContents){
                return
            }
            viewModelScope.launch {
                // stop dataload job
                _dataLoadJob?.cancel()
                _dataLoadJob?.join()
                _dataLoadJob = null

                _gridContents = gridContents
                _itemList.clear()
                lastDataSize = dataSize.value
                _dataLoadJob = null
                _dataSize.emit(0)
                _loading.emit(false)
                loadNextImageList()
            }
        }
        else{
            // photoRepositoryが決定してから再度setGridContentsを呼び出すために、gridContentの値は保持しておく
            _gridContents = gridContents
        }
    }

    fun loadNextImageList() {
        if((_accountState.photoRepository.value != null) && (_dataLoadJob == null) && (_gridContents != null)){
            _dataLoadJob = viewModelScope.launch {
                _loading.emit(true)
                val photoMetaList = _accountState.photoRepository.value!!.getPhotoMetadataList(
                    _itemList.size, _readPageSize, _gridContents!!.searchQuery
                )
                _itemList.addAll(photoMetaList.map { PhotoGridItem(it) })
                lastDataSize = _dataSize.value
                _dataSize.emit(_dataSize.value + photoMetaList.size)
                _dataLoadJob = null
                _loading.emit(false)
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
                var bmp: Bitmap? = null
                withContext(Dispatchers.IO) {
                    bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                        photoGridItem.photoMetaData,
                        width,
                        height,
                        true
                    )
                }
                if(bmp != null) {
                    callback.invoke(bmp)
                }
            }
        }
    }

    fun goBack(){
        publishAction(SideBarAction(
            SideBarActionType.BACK
        ))
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
