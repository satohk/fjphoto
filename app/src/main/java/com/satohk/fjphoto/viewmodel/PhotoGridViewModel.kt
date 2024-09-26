package com.satohk.fjphoto.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.*
import com.satohk.fjphoto.repository.data.PhotoMetadata
import com.satohk.fjphoto.repository.data.PhotoMetadataLocal
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger


typealias PhotoGridItem = PhotoMetadata

class PhotoGridViewModel(
    private val _accountState: AccountState,
    private val _photoMetadataLocalRepository: PhotoMetadataLocalRepository
) : SideBarActionPublisherViewModel() {
    private val _filteredPhotoList: FilteredPhotoList by KoinJavaComponent.inject(FilteredPhotoList::class.java)
    private var _gridContents: GridContents? = null
    val gridContents: GridContents? get() = _gridContents
    private val _readPageSize = 10
    private val _readNum = 6
    private val _readPageSizeWhenAiFilterEnabled = 1
    private val _readNumWhenAiFilterEnabled = 60
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
    private val _launchBitmapLoadJob = MutableSharedFlow<Boolean>()

    private var _numThumbnailLoadingJobs: AtomicInteger = AtomicInteger(0)
    private val _thumbnailLoadWaitJobs = ConcurrentLinkedQueue<ThumbnailLoadJob>()
    val THUMBNAIL_LOADING_JOBS_MAX = 10
    val THUMBNAIL_LOAD_WAIT_JOBS_MAX = 50

    var isSelectMode: Boolean = false

    var onChangeToPhotoViewListener: ((gridContents:GridContents, autoPlay:Boolean, position:Int) -> Unit)? = null

    val gridItemList = PhotoGridItemList()

    init{
        _accountState.photoLoader.onEach {
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

        _launchBitmapLoadJob.onEach {
            Log.d("_launchBitmapLoadJob.onEach", "_numThumbnailLoadingJobs=${_numThumbnailLoadingJobs.get()}")
            var loadWaitJobs = "";
            for(param in _thumbnailLoadWaitJobs){
                loadWaitJobs += param.position.toString() + ","
            }
            Log.d("_launchBitmapLoadJob.onEach", "_thumbnailLoadWaitJobs=${loadWaitJobs}")
            if(_numThumbnailLoadingJobs.get() < THUMBNAIL_LOADING_JOBS_MAX){
                val loadParam = _thumbnailLoadWaitJobs.poll()
                if(loadParam != null){
                    viewModelScope.launch{
                        _numThumbnailLoadingJobs.incrementAndGet()
                        Log.d("loadThumbnail", "job#${_numThumbnailLoadingJobs.get()} load ${loadParam.position}")
                        val bmp: Bitmap? = withContext(Dispatchers.IO) {
                            _accountState.photoLoader.value?.getPhotoBitmap(
                                loadParam.photoMetadata.metadataRemote,
                                loadParam.width,
                                loadParam.height,
                                true
                            )
                        }
                        bmp?.let {
                            loadParam.callback.invoke(loadParam.position, it)
                        }
                        _numThumbnailLoadingJobs.decrementAndGet()
                        _launchBitmapLoadJob.emit(true)
                        Log.d("loadThumbnail", "exit job#${_numThumbnailLoadingJobs.get()} load ${loadParam.position}")
                    }
                }
            }
        }.launchIn(viewModelScope)
    }


    fun setGridContents(gridContents: GridContents){
        Log.d("PhotoGridViewModel", "setGridContents gridContents=$gridContents _gridContents=$_gridContents photoRepository=${_accountState.photoLoader.value}")
        if(_accountState.photoLoader.value != null) {
            if(gridContents == _gridContents){
                return
            }

            viewModelScope.launch {
                _gridContents = gridContents
                _filteredPhotoList.setParameter(_accountState.photoLoader.value!!, gridContents.searchQuery)
                gridItemList._filteredPhotoList = _filteredPhotoList
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
        if((_accountState.photoLoader.value != null) && (_gridContents != null) && (gridItemList._filteredPhotoList != null)){
            viewModelScope.launch {
                val readNum = if(_gridContents!!.searchQuery.queryLocal.aiFilterEnabled) _readNumWhenAiFilterEnabled
                                else _readNum
                val readPageSize = if(_gridContents!!.searchQuery.queryLocal.aiFilterEnabled) _readPageSizeWhenAiFilterEnabled
                    else _readPageSize

                var skipped = false
                _loading.emit(true)
                for(i in 1..readNum) {
                    skipped = !gridItemList.loadNext(readPageSize)
                    if(!skipped) {
                        _dataSize.emit(gridItemList._filteredPhotoList!!.size)
                    }
                }
                if(!skipped) {
                    _loading.emit(false)
                }
            }
        }
        else{
            Log.d("loadNextImageList", "_accountState.photoRepository.value is null")
        }
    }

    fun loadThumbnail(photoMetadata: PhotoMetadata, width:Int?, height:Int?, position:Int, callback:(position:Int, bmp:Bitmap?)->Unit) {
        Log.d("loadThumbnail", "enter   position $position")
        if (_accountState.photoLoader.value != null) {
            while(_thumbnailLoadWaitJobs.size >= THUMBNAIL_LOAD_WAIT_JOBS_MAX){
                val removedJob = _thumbnailLoadWaitJobs.poll()
                Log.d("loadThumbnailJob", "remove   position=${removedJob.position}")
            }
            _thumbnailLoadWaitJobs.add(
                ThumbnailLoadJob(
                    photoMetadata,
                    width,
                    height,
                    position,
                    callback
                )
            )
            viewModelScope.launch {
                _launchBitmapLoadJob.emit(true)
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
                    PhotoMetadataLocal(it[position].metadataRemote.id, !it[position].metadataLocal.favorite),
                    it[position].metadataRemote
                )
                it[position] = newItem
                viewModelScope.launch {
                    _photoMetadataLocalRepository.set(
                        newItem.metadataRemote.id,
                        _accountState.activeAccount.value!!.accountId,
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
                _accountState.settingRepository.set(screensaverSearchQuery = it.searchQuery)
            }
        }
    }

    class ThumbnailLoadJob (
        internal val photoMetadata: PhotoMetadata,
        internal val width:Int?,
        internal val height:Int?,
        internal val position:Int,
        internal val callback:(position:Int, bmp:Bitmap?)->Unit
    ){}

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

        suspend fun loadNext(count:Int): Boolean{
            var res = false
            _filteredPhotoList?.let {
                res = it.loadNext(count, false)
                size = it.size
            }
            return res
        }

        operator fun get(i:Int):PhotoGridItem?{
            val res: PhotoGridItem? = try {
                _filteredPhotoList!![i]
            }
            catch(e: java.lang.IndexOutOfBoundsException){
                // Depending on the fragment switching timing, _photoList may have been cleared
                // so IndexOutOfBoundsException may occur.
                null
            }
            return res
        }
        var size:Int = 0
            private set
    }
}
