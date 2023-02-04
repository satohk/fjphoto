package com.satohk.fjphoto.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import com.satohk.fjphoto.domain.FilteredPhotoList
import com.satohk.fjphoto.domain.PhotoSelector
import com.satohk.fjphoto.repository.data.PhotoMetadata
import com.satohk.fjphoto.repository.remoterepository.CachedPhotoRepository
import kotlinx.coroutines.launch
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.satohk.fjphoto.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.java.KoinJavaComponent


class PhotoViewModel(
    private val _accountState: AccountState
) : ViewModel() {

    data class Media (
        val bitmap: Bitmap?,
        val videoUrl: String?,
        val photoMetadata: PhotoMetadata?,
        val viewIndex: Int,
    )

    enum class SlideShowOrder(val index:Int){
        SEQUENTIAL(0),
        RANDOM(1)
    }

    private val _currentMedia = MutableStateFlow<Media?>(null)
    val currentMedia: StateFlow<Media?> get() = _currentMedia
    private val _preparingMedia = MutableStateFlow<Media?>(null)
    val preparingMedia: StateFlow<Media?> get() = _preparingMedia
    private val _errorMessageId = MutableSharedFlow<Int>()
    val errorMessageId: SharedFlow<Int?> get() = _errorMessageId
    private val _reprepareMedia = MutableSharedFlow<Boolean>()
    val reprepareMedia: SharedFlow<Boolean> get() = _reprepareMedia
    private val _showProgressBar = MutableStateFlow(false)
    val showProgressBar: StateFlow<Boolean> get() = _showProgressBar

    var photoWidth: Int = 1024
    var photoHeight: Int = 768

    private val _filteredPhotoList: FilteredPhotoList by KoinJavaComponent.inject(FilteredPhotoList::class.java)
    private var _photoSelector: PhotoSelector? = null
    private var _stopped = false
    private val _mutex = Mutex()

    var gridContents: GridContents? = null
    var isSlideShow: Boolean = true
    var showIndex: Int = 0

    val muteVideoPlayer: Boolean
        get() = isSlideShow && _accountState.settingRepository.setting.value.slideShowMute

    val videoPlayerEventListener = object : Player.Listener {
        //Player events
        override fun onPlayerError(error: PlaybackException) {
            Log.d(
                "PhotoViewModel",
                "onPlayerStateChanged $error, ${error.cause.toString()}"
            )
            viewModelScope.launch {
                _errorMessageId.emit(
                    R.string.msg_video_load_error
                )

                if (this@PhotoViewModel.isSlideShow) {
                    this@PhotoViewModel.goNext()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(
                "PhotoViewModel",
                "onPlayerStateChanged playbackState=$playbackState"
            )
            if (playbackState == Player.STATE_READY) {
                Log.d("PhotoViewModel", "onPlayerStateChanged Player.STATE_READY")
                if(_preparingMedia.value != null) {
                    setCurrentMedia(_preparingMedia.value)
                }
            } else if (playbackState == Player.STATE_ENDED) {
                if (this@PhotoViewModel.isSlideShow && !this@PhotoViewModel._accountState.settingRepository.setting.value.slideShowCutPlay) {
                    this@PhotoViewModel.goNext()
                }
            } else if (playbackState == Player.STATE_IDLE) {
                Log.d("PhotoViewModel", "onPlayerStateChanged Player.STATE_IDLE")
                viewModelScope.launch{
                    //_reprepareMedia.emit(true)
                }
            }
        }
    }

    init{
        _accountState.photoRepository.onEach {
            if((it != null) && (gridContents != null)){
                start()
            }
        }.launchIn(viewModelScope)
    }

    fun start(){
        Log.d("PhotoViewModel", "" +
                "start() _accountState.photoRepository.value=${_accountState.photoRepository.value}"
                + " gridContents=$gridContents")
        stop()

        if(_accountState.photoRepository.value == null) {
            return
        }
        if(gridContents == null) {
            return
        }

        // init variables
        _currentMedia.value = null
        _preparingMedia.value = null

        viewModelScope.launch {
            initPhotoSelector(
                _accountState.photoRepository.value!!,
                gridContents!!,
                showIndex
            )
        }
    }

    fun stop(){
        Log.d("PhotoViewModel", "stop()")
        if(_photoSelector != null) {
            _photoSelector = null
        }
        _stopped = true
    }

    fun goNext(waitTime: Long = 0){
        if(_photoSelector != null && !_stopped) {
            viewModelScope.launch {
                _photoSelector!!.goNext(waitTime)
            }
        }
    }

    fun goPrev(){
        if(_photoSelector != null && !_stopped) {
            viewModelScope.launch {
                _photoSelector!!.goPrev()
            }
        }
    }

    private fun getNextViewIndex(): Int{
        Log.d("PhotoViewModel", "getNextViewIndex currentMedia=${_currentMedia.value}")
        val current = _currentMedia.value?.viewIndex ?: 0
        return (current + 1) % 2
    }

    private suspend fun prepareMedia(metadata: PhotoMetadata) {
        Log.d("PhotoViewModel", "prepareMedia:${metadata.metadataRemote.id}")

        _mutex.withLock {
            if(_preparingMedia.value == null) {
                if (metadata.metadataRemote.mimeType.startsWith("image")) {
                    viewModelScope.launch {
                        val bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                            metadata.metadataRemote,
                            photoWidth,
                            photoHeight,
                            false
                        )
                        Log.d("PhotoViewModel", "prepareMedia: bmp loaded")
                        setCurrentMedia(
                            Media(
                                bmp,
                                null,
                                metadata,
                                _preparingMedia.value!!.viewIndex
                            )
                        )
                    }
                    _preparingMedia.value = Media(null, null, metadata, getNextViewIndex())
                } else if (metadata.metadataRemote.mimeType.startsWith("video")) {
                    val tmp =
                        _accountState.photoRepository.value!!.getMediaAccessHeaderAndUrl(metadata.metadataRemote)
                    _preparingMedia.value = Media(null, tmp.second, metadata, getNextViewIndex())
                }
            }
        }

        if(!isSlideShow){
            _showProgressBar.value = true
        }
    }

    private fun setCurrentMedia(media: Media?){
        Log.d("PhotoViewModel", "setCurrentMedia media.viewIndex:${media?.viewIndex}")

        if(_currentMedia.value?.photoMetadata?.metadataRemote?.id == media?.photoMetadata?.metadataRemote?.id){
            return
        }
        _preparingMedia.value = null
        _currentMedia.value = media
        _showProgressBar.value = false

        if(isSlideShow &&
            (_accountState.settingRepository.setting.value.slideShowCutPlay || _currentMedia.value?.bitmap != null) ){
            this.goNext(_accountState.settingRepository.setting.value.slideShowInterval.toLong() * 1000)
        }
    }

    private suspend fun initPhotoSelector(repo: CachedPhotoRepository, contents:GridContents, showIndex: Int){
        Log.d("PhotoViewModel", "initPhotoSelector() repo=$repo, contents=$contents, showIndex=$showIndex")

        _filteredPhotoList.setParameter(repo, contents.searchQuery)
        _photoSelector = PhotoSelector(
            _filteredPhotoList,
            if(!isSlideShow || _accountState.settingRepository.setting.value.slideShowOrder == SlideShowOrder.SEQUENTIAL.index)
                PhotoSelector.SelectMode.SEQUENTIAL
            else
                PhotoSelector.SelectMode.RANDOM
                ,
            showIndex
        )

        _photoSelector!!.currentPhotoMetadata.onEach { it ->
            Log.d("PhotoViewModel", "_photoSelector!!.currentPhotoMetadata.onEach it:(${it?.first?.metadataRemote?.id},${it?.second?.metadataRemote?.id})")

            it?.let { metadataPair: Pair<PhotoMetadata, PhotoMetadata> ->
                prepareMedia(metadataPair.first)
            }
        }.launchIn(viewModelScope)

        _stopped = false
        this.goNext()
        viewModelScope.launch {
            loadAllMetadata()
        }
    }

    private suspend fun loadAllMetadata(){
        Log.d("PhotoViewModel", "loadAllMetadata started")
        while(!_filteredPhotoList.allLoaded && !_stopped){
            _filteredPhotoList.loadNext(100, false)
            delay(500)
            Log.d("PhotoViewModel", "loadAllMetadata _filteredPhotoList.size=${_filteredPhotoList.size}")
        }
        Log.d("PhotoViewModel", "loadAllMetadata ended")
    }
}
