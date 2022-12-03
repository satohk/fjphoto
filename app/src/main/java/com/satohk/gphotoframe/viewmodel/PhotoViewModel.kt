package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.domain.FilteredPhotoList
import com.satohk.gphotoframe.domain.PhotoSelector
import com.satohk.gphotoframe.repository.data.PhotoMetadata
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.satohk.gphotoframe.R
import kotlinx.coroutines.flow.*


class PhotoViewModel(
    private val _accountState: AccountState
) : ViewModel(), Player.Listener {

    data class Media (
        val bitmap: Bitmap?,
        val videoPlayer: ExoPlayer?
    )

    private lateinit var _videoPlayers: List<ExoPlayer>
    private val _currentMedia = MutableStateFlow(Media(null, null))
    val currentMedia: StateFlow<Media> get() = _currentMedia
    private var _currentPlayer: ExoPlayer? = null
    var photoWidth: Int = 1024
    var photoHeight: Int = 768
    var currentPhotoMetadata: PhotoMetadata? = null
        private set

    private var _gridContents: GridContents? = null

    private var _filteredPhotoList: FilteredPhotoList? = null
    private var _photoSelector: PhotoSelector? = null
    private var _slideShow: Boolean = true
    private var _showIndex: Int = 0

    private val _errorMessageId = MutableSharedFlow<Int>()
    val errorMessageId: SharedFlow<Int?> get() = _errorMessageId

    init{
        _accountState.photoRepository.onEach {
            if((it != null) && (_gridContents != null)){
                initPhotoSelector(it, _gridContents!!, _slideShow, _showIndex)
            }
        }.launchIn(viewModelScope)
    }

    fun setVidePlayers(videoPlayers: List<ExoPlayer>){
        _videoPlayers = videoPlayers
        for(player in videoPlayers){
            player.addListener(this)
        }
    }

    private fun getAvailablePlayer(): ExoPlayer {
        for(player in _videoPlayers){
            if(_currentMedia.value.videoPlayer != player){
                return player
            }
        }
        check(false) { "This code won't run" }
        return _videoPlayers[0]  // "This code won't run"
    }

    fun onStart(gridContents: GridContents?, slideShow: Boolean, showIndex: Int){
        if(_accountState.photoRepository.value != null){
            if (gridContents != null) {
                this._gridContents = gridContents
            }
            else{
                this._gridContents = GridContents(
                    _accountState.settingRepository.setting.value.screensaverSearchQuery
                )
            }
            _slideShow = slideShow
            _showIndex = showIndex
            initPhotoSelector(_accountState.photoRepository.value!!, this._gridContents!!, _slideShow, _showIndex)
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

    private fun initPhotoSelector(repo: CachedPhotoRepository, contents:GridContents, slideShow: Boolean, showIndex: Int){
        onStop()

        _filteredPhotoList = FilteredPhotoList(repo, contents.searchQuery)
        _photoSelector = PhotoSelector(
            _filteredPhotoList!!,
            PhotoSelector.SelectMode.SEQUENTIAL,
            10000,
            showIndex
        )

        _photoSelector!!.currentPhotoMetadata.onEach { it ->
            it?.let{ metadata: PhotoMetadata ->
                currentPhotoMetadata = metadata
                Log.d("currentPhotoMetadata", currentPhotoMetadata.toString())
                if(metadata.metadataRemote.mimeType.startsWith("image")) {
                    withContext(Dispatchers.IO) {
                        val bmp = _accountState.photoRepository.value!!.getPhotoBitmap(
                            metadata.metadataRemote,
                            photoWidth,
                            photoHeight,
                            false
                        )
                        _currentMedia.value = Media(bmp, null)
                    }
                }
                else if(metadata.metadataRemote.mimeType.startsWith("video")){
                    val tmp = _accountState.photoRepository.value!!.getMediaAccessHeaderAndUrl(metadata.metadataRemote)
                    val mediaUrl = tmp.second
                    val mediaItem = MediaItem.fromUri(mediaUrl)
                    _currentPlayer = getAvailablePlayer()
                    _currentPlayer?.let { player ->
                        player.setMediaItem(mediaItem)
                        player.playWhenReady = true
                        player.prepare()
                        if(!this._slideShow){ // Change media immediately if in manual mode
                            _currentMedia.value = Media(null, _currentPlayer)
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _photoSelector!!.start(slideShow)
        }
    }

    //Player events
    override fun onPlayerError(error: PlaybackException) {
        Log.d("onPlayerError", "${error.toString()}, ${error.cause.toString()}")
        viewModelScope.launch {
            _errorMessageId.emit(
                R.string.msg_video_load_error
            )
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d("onPlayerStateChanged", "playWhenReady=${playWhenReady}, playbackState=${playbackState}")
        if(playbackState == ExoPlayer.STATE_READY){
            if(this._slideShow) { // Change media when ready if in slideshow mode
                _currentMedia.value = Media(null, _currentPlayer)
            }
        }
    }
}
