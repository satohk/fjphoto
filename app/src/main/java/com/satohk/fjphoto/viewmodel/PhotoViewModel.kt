package com.satohk.fjphoto.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import com.satohk.fjphoto.domain.FilteredPhotoList
import com.satohk.fjphoto.domain.PhotoSelector
import com.satohk.fjphoto.domain.CachedPhotoLoader
import kotlinx.coroutines.launch
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.satohk.fjphoto.repository.data.PhotoMetadata
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent
import java.time.format.DateTimeFormatter


class PhotoViewModel(
    private val _accountState: AccountState
) : ViewModel() {

    data class Media(
        val photo: Bitmap? = null,
        val videoUrl: String? = null,
        val index: Int = 0,
        val info: String = "",
        val fadeInDuration: Int
    )

    private val _errorMessageId = MutableSharedFlow<Int>()
    val errorMessageId: SharedFlow<Int?> get() = _errorMessageId
    private val _showProgressBar = MutableStateFlow(false)
    val showProgressBar: StateFlow<Boolean> get() = _showProgressBar
    private val _currentMedia = MutableStateFlow(Media(fadeInDuration = 0))
    val currentMedia: StateFlow<Media> get() = _currentMedia
    private val _prepareMedia = MutableStateFlow(Media(fadeInDuration = 0))
    val prepareMedia: StateFlow<Media> get() = _prepareMedia

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
        _accountState.photoLoader.onEach {
            if((it != null) && (gridContents != null)){
                start()
            }
        }.launchIn(viewModelScope)
    }

    fun start(){
        Log.d("PhotoViewModel", "" +
                "start() _accountState.photoRepository.value=${_accountState.photoLoader.value}"
                + " gridContents=$gridContents")
        stop()

        if(_accountState.photoLoader.value == null) {
            return
        }
        if(gridContents == null) {
            return
        }

        viewModelScope.launch {
            initPhotoSelector(
                _accountState.photoLoader.value!!,
                gridContents!!,
                showIndex
            )
        }
    }

    fun stop(){
        Log.d("PhotoViewModel", "stop()")
        _photoSelector?.stop()
        _photoSelector = null
        _currentMedia.value = Media(fadeInDuration = 0)
        _prepareMedia.value = Media(fadeInDuration = 0)
    }

    fun goNext(){
        viewModelScope.launch {
            _photoSelector?.goNext()
        }
    }

    fun goPrev(){
        viewModelScope.launch {
            _photoSelector?.goPrev()
        }
    }

    private fun metadataInfoText(metadata: PhotoMetadata?): String{
        return if(metadata != null) {
            metadata.metadataRemote.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } else{
            ""
        }
    }

    private suspend fun prepareMedia(mediaMetadata: PhotoSelector.MediaMetadata) {
        when(mediaMetadata.mediaType) {
            PhotoSelector.MediaType.PHOTO -> {
                Log.d("PhotoViewModel", "prepareMedia bitmap mediaIndex=${mediaMetadata.index}")
                viewModelScope.launch {
                    val bmp = _accountState.photoLoader.value!!.getPhotoBitmap(
                        mediaMetadata.metadata!!.metadataRemote,
                        photoWidth,
                        photoHeight,
                        false
                    )
                    val media = Media(bmp, null, mediaMetadata.index,
                        metadataInfoText(mediaMetadata.metadata), fadeInDuration = mediaMetadata.fadeinDuration)
                    _currentMedia.emit(media)
                }
            }
            PhotoSelector.MediaType.VIDEO -> {
                Log.d("PhotoViewModel", "prepareMedia video mediaIndex=${mediaMetadata.index}")
                viewModelScope.launch {
                    val tmp =
                        _accountState.photoLoader.value!!.getMediaAccessHeaderAndUrl(mediaMetadata.metadata!!.metadataRemote)
                    val media = Media(
                        null,
                        tmp.second,
                        mediaMetadata.index,
                        metadataInfoText(mediaMetadata.metadata),
                        fadeInDuration = mediaMetadata.fadeinDuration
                    )
                    _prepareMedia.emit(media)
                }
            }
            PhotoSelector.MediaType.BLANK -> {
                Log.d("PhotoViewModel", "prepareMedia Blank mediaIndex=${mediaMetadata.index}")
                _currentMedia.emit(Media(null, null, mediaMetadata.index,
                    metadataInfoText(mediaMetadata.metadata), fadeInDuration = mediaMetadata.fadeinDuration))
            }
        }
    }

    private suspend fun initPhotoSelector(repo: CachedPhotoLoader, contents:GridContents, showIndex: Int){
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

        _photoSelector!!.currentMedia.onEach {
            Log.d("PhotoViewModel", "_photoSelector!!.currentMedia.onEach mediaIndex=${it.index}")
            prepareMedia(it)
        }.launchIn(viewModelScope)

        _photoSelector!!.isLoading.onEach {
            if(!isSlideShow) {
                this._showProgressBar.value = it
            }
            else{
                this._showProgressBar.value = false
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch{
            _photoSelector?.startSlideshow()
        }
        viewModelScope.launch {
            _photoSelector?.loadAllMetadata()
        }
    }

    fun onMediaStarted(mediaIndex: Int){
        Log.d(
            "PhotoViewModel",
            "onMediaStarted mediaIndex=$mediaIndex"
        )
        viewModelScope.launch {
            _photoSelector?.onMediaStarted(mediaIndex)
        }
    }

    // VideoPlayer Event Listener
    fun onPlayerError(error: PlaybackException, mediaIndex: Int) {
        Log.d(
            "PhotoViewModel",
            "onPlayerStateChanged mediaIndex=$mediaIndex, $error, ${error.cause.toString()}"
        )
        viewModelScope.launch { _photoSelector?.onMediaError(mediaIndex) }
    }

    fun onPlaybackStateChanged(playbackState: Int,  mediaIndex: Int) {
        Log.d(
            "PhotoViewModel",
            "onPlayerStateChanged mediaIndex=$mediaIndex, playbackState=$playbackState (IDLE=1, BUFFERING=2, READY=3, ENDED=4)"
        )
        when(playbackState){
            Player.STATE_READY -> {
                if(mediaIndex == _photoSelector?.currentMedia?.value?.index) {
                    viewModelScope.launch {
                        _currentMedia.emit(_prepareMedia.value)
                    }
                }
            }
            Player.STATE_ENDED -> {
                viewModelScope.launch { _photoSelector?.onMediaEnded(mediaIndex) }
            }
            Player.STATE_IDLE -> {
            }
        }
    }
}
