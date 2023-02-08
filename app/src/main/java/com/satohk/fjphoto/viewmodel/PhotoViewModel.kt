package com.satohk.fjphoto.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import com.satohk.fjphoto.domain.FilteredPhotoList
import com.satohk.fjphoto.domain.PhotoSelector
import com.satohk.fjphoto.repository.remoterepository.CachedPhotoRepository
import kotlinx.coroutines.launch
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.satohk.fjphoto.repository.remoterepository.Photo
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent


class PhotoViewModel(
    private val _accountState: AccountState
) : ViewModel() {

    private val _errorMessageId = MutableSharedFlow<Int>()
    val errorMessageId: SharedFlow<Int?> get() = _errorMessageId
    private val _showProgressBar = MutableStateFlow(false)
    val showProgressBar: StateFlow<Boolean> get() = _showProgressBar
    private val _fadeInMedia = MutableSharedFlow<PhotoSelector.Media?>(1)
    val fadeInMedia = _fadeInMedia.asSharedFlow()
    private val _loadMedia = MutableSharedFlow<PhotoSelector.Media?>(1)
    val loadMedia = _loadMedia.asSharedFlow()

    var photoWidth: Int = 1024
    var photoHeight: Int = 768

    private val _filteredPhotoList: FilteredPhotoList by KoinJavaComponent.inject(FilteredPhotoList::class.java)
    private var _photoSelector: PhotoSelector? = null

    var gridContents: GridContents? = null
    var isSlideShow: Boolean = true
    var showIndex: Int = 0

    val muteVideoPlayer: Boolean
        get() = isSlideShow && _accountState.settingRepository.setting.value.slideShowMute

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
        _photoSelector?.stop()
        _photoSelector = null
    }

    fun goNext(waitTime: Long = 0){
        viewModelScope.launch {
            _photoSelector?.goNext(waitTime)
        }
    }

    fun goPrev(){
        viewModelScope.launch {
            _photoSelector?.goPrev()
        }
    }

    private fun prepareMedia(media: PhotoSelector.Media?) {
        Log.d("PhotoViewModel", "prepareMedia:${media?.photoMetadata?.metadataRemote?.id}")

        if(media?.photoMetadata != null) {
            if (media.photoMetadata.metadataRemote.mimeType.startsWith("image")) {
                Log.d("PhotoViewModel", "prepareMedia bitmap")
                viewModelScope.launch {
                    val bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                        media.photoMetadata.metadataRemote,
                        photoWidth,
                        photoHeight,
                        false
                    )
                    media.bitmap = bmp
                    media.state = PhotoSelector.Media.State.LOADED
                    _photoSelector?.onMediaStateChanged()
                }
            } else if (media.photoMetadata.metadataRemote.mimeType.startsWith("video")) {
                Log.d("PhotoViewModel", "prepareMedia video")
                val tmp =
                    _accountState.photoRepository.value!!.getMediaAccessHeaderAndUrl(media.photoMetadata.metadataRemote)

                media.videoUrl = tmp.second
                viewModelScope.launch {
                    Log.d("PhotoViewModel", "prepareMedia video _loadMedia=${media.mediaId}")
                    _loadMedia.emit(media)
                }
            }
            media.state = PhotoSelector.Media.State.LOADING
        }
    }

    private suspend fun initPhotoSelector(repo: CachedPhotoRepository, contents:GridContents, showIndex: Int){
        Log.d("PhotoViewModel", "initPhotoSelector() repo=$repo, contents=$contents, showIndex=$showIndex")

        _filteredPhotoList.setParameter(repo, contents.searchQuery)
        _photoSelector = PhotoSelector(
            _filteredPhotoList,
            if(!isSlideShow || _accountState.settingRepository.setting.value.slideShowOrder == PhotoSelector.SelectMode.SEQUENTIAL.index)
                PhotoSelector.SelectMode.SEQUENTIAL
            else
                PhotoSelector.SelectMode.RANDOM
                ,
            showIndex,
            isSlideShow,
            _accountState.settingRepository.setting.value.slideShowCutPlay,
            _accountState.settingRepository.setting.value.slideShowInterval.toLong() * 1000
        )

        _photoSelector!!.prepareMedia.onEach { it ->
            Log.d("PhotoViewModel", "_photoSelector!!.prepareMedia.onEach it:(${it?.photoMetadata?.metadataRemote?.id})")
            prepareMedia(it)
        }.launchIn(viewModelScope)
        _photoSelector!!.showMedia.onEach { it ->
            Log.d("PhotoViewModel", "_photoSelector!!.showMedia.onEach it:(${it?.photoMetadata?.metadataRemote?.id})")
            it?.let{ media ->
                media.state = PhotoSelector.Media.State.FADE_IN
                _fadeInMedia.emit(media)
            }
        }.launchIn(viewModelScope)
        _photoSelector!!.preparingCurrentMedia.onEach {
            Log.d("PhotoViewModel", "_photoSelector!!.preparingCurrentMedia it:$it)")
            _showProgressBar.value = it && (!isSlideShow)
        }.launchIn(viewModelScope)

        this.goNext()
        viewModelScope.launch {
            _photoSelector?.loadAllMetadata()
        }
    }

    fun onFadedIn(media: PhotoSelector.Media){
        media.state = PhotoSelector.Media.State.SHOWING
        viewModelScope.launch { _photoSelector?.onMediaStateChanged() }
    }

    // VideoPlayer Event Listener
    fun onPlayerError(error: PlaybackException, media: PhotoSelector.Media) {
        Log.d(
            "PhotoViewModel",
            "onPlayerStateChanged $error, ${error.cause.toString()} media=${media.mediaId}"
        )
        media.state = PhotoSelector.Media.State.ERROR
        viewModelScope.launch { _photoSelector?.onMediaStateChanged() }
    }

    fun onPlaybackStateChanged(playbackState: Int, media: PhotoSelector.Media) {
        Log.d(
            "PhotoViewModel",
            "onPlayerStateChanged playbackState=$playbackState media=${media.mediaId}"
        )
        when(playbackState){
            Player.STATE_READY -> {
                media.state = PhotoSelector.Media.State.LOADED
                viewModelScope.launch { _photoSelector?.onMediaStateChanged() }
            }
            Player.STATE_ENDED -> {
                media.state = PhotoSelector.Media.State.ENDED
                viewModelScope.launch { _photoSelector?.onMediaStateChanged() }
            }
            Player.STATE_IDLE -> {
                media.state = PhotoSelector.Media.State.BEFORE_LOAD
                viewModelScope.launch { _photoSelector?.onMediaStateChanged() }
            }
        }
    }
}
