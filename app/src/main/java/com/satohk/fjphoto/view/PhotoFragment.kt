package com.satohk.fjphoto.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.satohk.fjphoto.R
import com.satohk.fjphoto.domain.PhotoSelector
import com.satohk.fjphoto.viewmodel.GridContents
import com.satohk.fjphoto.viewmodel.PhotoViewModel
import kotlinx.android.synthetic.main.fragment_photo.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.time.format.DateTimeFormatter


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoFragment() : Fragment(R.layout.fragment_photo) {
    private val _viewModel by sharedViewModel<PhotoViewModel>()
    private val _imageViews = mutableListOf<ImageView>()
    private val _videoViews = mutableListOf<StyledPlayerView>()
    private val _videoPlayers = mutableListOf<ExoPlayer>()
    private var _currentMediaView: View? = null
    private var _videoPlayerListener = mutableListOf<VideoPlayerListener>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PhotoFragment", "onViewCreated")

        if(arguments != null) {
            _viewModel.isSlideShow = requireArguments().getBoolean("slideShow")
            _viewModel.showIndex = requireArguments().getInt("showIndex")
            _viewModel.gridContents = requireArguments().get("contents") as GridContents?
        }

        val progressBar = view.findViewById<ProgressBar>(R.id.progress)
        _imageViews.clear()
        _imageViews.add(view.findViewById(R.id.imageView1))
        _imageViews.add(view.findViewById(R.id.imageView2))
        _videoViews.clear()
        _videoViews.add(view.findViewById(R.id.videoView1))
        _videoViews.add(view.findViewById(R.id.videoView2))
        for(imageView in _imageViews){
            imageView.setImageResource(R.drawable.blank_image)
            imageView.visibility = View.INVISIBLE
            imageView.alpha = 0.0f
        }
        for(videoView in _videoViews) {
            videoView.controllerAutoShow = false
            videoView.visibility = View.INVISIBLE
            videoView.alpha = 0.0f
        }
        initializeVideoPlayer()

        // focusをコントロールするためのダミーボタン
        val dummyButton = view.findViewById<Button>(R.id.dummyButtonTop)
        dummyButton.setOnKeyListener { _: View, _: Int, keyEvent: KeyEvent ->
            Log.d("PhotoFragment", "KeyEvent " + keyEvent.toString())
            if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                if(_currentMediaView is StyledPlayerView){
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> (_currentMediaView as StyledPlayerView).showController()
                        KeyEvent.KEYCODE_DPAD_LEFT -> _viewModel.goPrev()
                        KeyEvent.KEYCODE_DPAD_RIGHT -> _viewModel.goNext()
                        else -> return@setOnKeyListener false
                    }
                    return@setOnKeyListener true
                }
                else {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> _viewModel.goPrev()
                        KeyEvent.KEYCODE_DPAD_RIGHT -> _viewModel.goNext()
                        else -> return@setOnKeyListener false
                    }
                    return@setOnKeyListener true
                }

            }
            return@setOnKeyListener false
        }

        dummyButton.setOnFocusChangeListener { _, b ->
            Log.d("PhotoFragment", "dummyButton focus $b")
            if(b) {
                if(_currentMediaView is StyledPlayerView){
                    (_currentMediaView as StyledPlayerView).hideController()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                    _viewModel.loadMedia.collect {
                        Log.d("PhotoFragment", "_viewModel.prepareVideo.collect ${it?.mediaId}")
                        prepareVideo(it)
                    }
                }
                launch{
                    _viewModel.fadeInMedia.collect {
                        Log.d("PhotoFragment", "_viewModel.swapMediaView.collect ${it?.mediaId}")
                        changeMedia(it)
                    }
                }
                launch {
                    _viewModel.errorMessageId.collect {
                        Log.d("PhotoFragment", "_viewModel.errorMessageId.collect $it")
                        it?.let { messageId ->
                            Toast.makeText(context, getText(messageId), Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    _viewModel.showProgressBar.collect {
                        Log.d("PhotoFragment", "_viewModel.showProgressBar.collect $it")
                        progressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onStart() {
        Log.d("PhotoFragment", "onStart")
        super.onStart()
        setPhotoSize()
        _viewModel.start()
    }

    override fun onStop() {
        Log.d("PhotoFragment", "onStop")
        super.onStop()
        releaseVidePlayers()
        _viewModel.stop()
    }

    private fun initializeVideoPlayer() {
        val minBufferDuration = 2000      //Minimum Video you want to buffer while Playing
        val maxBufferDuration = 10000     //Max Video you want to buffer during PlayBack
        val minPlaybackStartBuffer = 100 //Min Video you want to buffer before start Playing it
        val minPlaybackResumeBuffer = 2000   //Min video You want to buffer when user resumes video

        _videoPlayers.clear()
        _videoPlayerListener.clear()
        for(videoView in _videoViews) {
            val loadControl: LoadControl = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, 16))
                .setBufferDurationsMs(
                    minBufferDuration,
                    maxBufferDuration,
                    minPlaybackStartBuffer,
                    minPlaybackResumeBuffer
                )
                .setTargetBufferBytes(-1)
                .setPrioritizeTimeOverSizeThresholds(true).createDefaultLoadControl()

            val trackSelector: TrackSelector = DefaultTrackSelector(this.requireContext())

            val player = ExoPlayer.Builder(this.requireContext())
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build()
                .also { exoPlayer ->
                    videoView.player = exoPlayer
                }
            val eventListener = VideoPlayerListener(_viewModel)
            _videoPlayerListener.add(eventListener)
            player.addListener(eventListener)
            player.volume = if(_viewModel.muteVideoPlayer) 0.0f else 1.0f
            _videoPlayers.add(player)
        }
    }

    private fun releaseVidePlayers() {
        Log.d("PhotoFragment", "releaseVideoPlayers")
        for(player in _videoPlayers) {
            player.release()
        }
    }

    private fun prepareVideo(media: PhotoSelector.Media?) {
        Log.d("PhotoFragment", "prepareVideo media=${media?.mediaId} player=${media?.viewIndex}")
        if(media?.videoUrl != null) {
            val mediaItem = MediaItem.fromUri(media.videoUrl!!)
            val player = _videoPlayers[media.viewIndex]
            if(player.isPlaying) {
                player.stop()
            }
            player.setMediaItem(mediaItem)
            player.playWhenReady = false
            _videoPlayerListener[media.viewIndex].media = media
            player.prepare()
        }
    }

    private fun changeMedia(media: PhotoSelector.Media?){
        Log.d("PhotoView", "changeMedia media.viewIndex=${media?.viewIndex}")

        val fadeDuration = if(_viewModel.isSlideShow) 1000L else 100 // msec
        val nextView: View = if(media?.bitmap != null){ // next media is bitmap
            _imageViews[media.viewIndex].setImageBitmap(media.bitmap)
            _imageViews[media.viewIndex]
        } else if(media?.videoUrl != null){ // next media is video
            _videoPlayers[media.viewIndex].play()
            _videoViews[media.viewIndex]
        } else { // next media is blank
            _imageViews[0].setImageResource(R.drawable.blank_image)
            _imageViews[0]
        }

        for(i in _videoViews.indices){
            _videoViews[i].focusable =
                if((media?.videoUrl != null) && (media.viewIndex == i)) View.FOCUSABLE
                else View.NOT_FOCUSABLE
        }

        nextView.alpha = 0.0f
        nextView.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(nextView, "alpha", 1.0f).apply {
            duration = fadeDuration
            start()
        }
        val prevView = _currentMediaView
        val currentViewIndex = media?.viewIndex ?: 0
        ObjectAnimator.ofFloat(_currentMediaView, "alpha", 0.0f).apply {
            addListener (object : AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator) {
                    prevView?.visibility = View.INVISIBLE
                    val playerIndex = (currentViewIndex + 1) % 2
                    if((_videoPlayers.size > playerIndex) && _videoPlayers[playerIndex].isPlaying) {
                        Log.d("PhotoFragment", "onAnimationEnd stop videoPlayer $playerIndex")
                        _videoPlayers[playerIndex].stop()
                    }
                    media?.let { _viewModel.onFadedIn(it) }
                }
            })
            duration = fadeDuration
            start()
        }

        media?.photoMetadata?.let {
            photoInfo.text =
                it.metadataRemote.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        _currentMediaView = nextView
    }

    private fun setPhotoSize(){
        _viewModel.photoWidth = _imageViews[0].width
        _viewModel.photoHeight = _imageViews[0].height
        Log.d("PhotoFragment", "setPhotoSize w:${_viewModel.photoWidth} h:${_viewModel.photoHeight}")
    }

    private class VideoPlayerListener(private val _viewModel:PhotoViewModel) : Player.Listener{
        var media: PhotoSelector.Media? = null

        override fun onPlayerError(error: PlaybackException) {
            Log.d("PhotoFragment", "onPlayerError error=$error media=${media?.mediaId}")
            _viewModel.onPlayerError(error, media!!)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d("PhotoFragment", "onPlaybackStateChanged playbackState=$playbackState media=${media?.mediaId}")
            _viewModel.onPlaybackStateChanged(playbackState, media!!)
        }
    }
}