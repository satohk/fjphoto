package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.*
import com.satohk.gphotoframe.repository.data.PhotoMetadata
import com.satohk.gphotoframe.repository.data.PhotoMetadataLocal
import com.satohk.gphotoframe.repository.localrepository.PhotoMetadataLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Filter


typealias PhotoGridItem = PhotoMetadata

class PhotoGridViewModel(
    private val _accountState: AccountState,
    private val _photoMetadataLocalRepository: PhotoMetadataLocalRepository
) : SideBarActionPublisherViewModel() {
    private var _gridContents: GridContents? = null
    val gridContents: GridContents? get() = _gridContents
    private val _readPageSize = 10
    private val _readNum = 6
    private var _dataLoadJob: Job? = null
    var focusIndex: Int = 0
        set(value){
            field = value
            if(value >= dataSize.value - 12) {
                loadNextImageList()
            }
        }
    var firstVisibleItemIndex: Int = 0

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val _dataSize = MutableStateFlow<Int>(0)
    val dataSize: StateFlow<Int> get() = _dataSize
    private val _numColumns = MutableStateFlow<Int>(6)
    val numColumns: StateFlow<Int> get() = _numColumns
    private val _changedItemIndex = MutableSharedFlow<Int>()
    val changedItemIndex = _changedItemIndex.asSharedFlow()

    var isSelectMode: Boolean = false

    var onChangeToPhotoViewListener: ((gridContents:GridContents, autoPlay:Boolean, position:Int) -> Unit)? = null

    val gridItemList = PhotoGridItemList()

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
        _accountState.settingRepository.setting.onEach {
            this._numColumns.value = it.numPhotoGridColumns
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
                gridItemList._filteredPhotoList = FilteredPhotoList(_accountState.photoRepository.value!!, gridContents.searchQuery)
                _dataLoadJob = null
                _dataSize.emit(0)
                _loading.emit(false)
                loadNextImageList()
            }
        }
        else{
            // photoRepositoryが決定してから再度setGridContentsを呼び出すために、gridContentの値は保持しておく
            _gridContents = gridContents
            gridItemList._filteredPhotoList = null
        }
    }

    private fun loadNextImageList() {
        Log.i("loadNextImageList", "Thread  = %s(%d)".format(Thread.currentThread().name, Thread.currentThread().id))
        if((_accountState.photoRepository.value != null) && (_dataLoadJob == null) && (_gridContents != null) && (gridItemList._filteredPhotoList != null)){
            _dataLoadJob = viewModelScope.launch {
                _loading.emit(true)
                for(i in 1.._readNum) {
                    gridItemList.loadNext(_readPageSize)
                    _dataSize.emit(gridItemList._filteredPhotoList!!.size)
                }
                _dataLoadJob = null
                _loading.emit(false)
            }
        }
        else{
            Log.d("loadNextImageList", "_accountState.photoRepository.value is null")
        }
    }

    fun loadThumbnail(photoMetadata: PhotoMetadata, width:Int?, height:Int?, callback:(bmp:Bitmap?)->Unit) {
        if(_accountState.photoRepository.value != null) {
            viewModelScope.launch {
                var bmp: Bitmap? = null
                withContext(Dispatchers.IO) {
                    bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                        photoMetadata.metadataRemote,
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

    fun onClickItem(position: Int){
        if(isSelectMode){
            gridItemList._filteredPhotoList?.let {
                val newItem = PhotoMetadata(
                    PhotoMetadataLocal(it[position].metadataRemote.id, it[position].metadataRemote.url, !it[position].metadataLocal.favorite),
                    it[position].metadataRemote
                )
                it[position] = newItem
                viewModelScope.launch {
                    _photoMetadataLocalRepository.set(
                        newItem.metadataRemote.id,
                        newItem.metadataLocal
                    )
                    _changedItemIndex.emit(position)
                }
            }
        }
        else {
            _gridContents?.run {
                onChangeToPhotoViewListener?.invoke(this, false, position)
            }
        }
    }

    fun onClickSlideshowButton(){
        _gridContents?.run {
            onChangeToPhotoViewListener?.invoke(this, true, 0)
        }
    }

    fun onClickSetToSlideshowButton(){
        _gridContents?.let{
            viewModelScope.launch {
                _accountState.settingRepository.setScreensaverSearchQuery(it.searchQuery)
            }
        }
    }

    class PhotoGridItemList{
        // If the list is changed in another thread and the size of the list changes before the RecyclerView is updated,
        // an error will occur, so save the list size

        internal var _filteredPhotoList: FilteredPhotoList? = null
            set(value){
                field = value
                size = if(_filteredPhotoList == null) {
                    0
                } else{
                    _filteredPhotoList!!.size
                }
            }

        suspend fun loadNext(count:Int){
            _filteredPhotoList?.let {
                it.loadNext(count)
                size = it.size
            }
        }

        operator fun get(i:Int):PhotoGridItem{
            return _filteredPhotoList!![i]
        }
        var size:Int = 0
            private set
    }
}
