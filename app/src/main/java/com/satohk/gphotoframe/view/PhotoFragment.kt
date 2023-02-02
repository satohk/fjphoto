package com.satohk.gphotoframe.view

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
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.GridContents
import com.satohk.gphotoframe.viewmodel.PhotoViewModel
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PhotoFragment", "onViewCreated")

        if(arguments != null) {
            _viewModel.isSlideShow = requireArguments().getBoolean("slideShow")
            _viewModel.showIndex = requireArguments().getInt("showIndex")
            _viewModel.gridContents = requireArguments().get("contents") as GridContents?
        }

        val progressBar = view.findViewById<ProgressBar>(R.id.progress)
        _imageViews.add(view.findViewById(R.id.imageView1))
        _imageViews.add(view.findViewById(R.id.imageView2))
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
                launch {
                    _viewModel.currentMedia.collect {
                        changeMedia(it)
                    }
                }
                launch {
                    _viewModel.preparingMedia.collect {
                        changePreparingMedia(it)
                    }
                }
                launch{
                    _viewModel.reprepareMedia.collect {
                        changePreparingMedia(_viewModel.preparingMedia.value)
                    }
                }
                launch {
                    _viewModel.errorMessageId.collect {
                        it?.let { messageId ->
                            Toast.makeText(context, getText(messageId), Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    _viewModel.showProgressBar.collect {
                        progressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onStart() {
        Log.d("PhotoFragment", "onStart")
        super.onStart()
        _viewModel.start()
    }

    override fun onStop() {
        Log.d("PhotoFragment", "onStop")
        super.onStop()
        releaseVidePlayer()
        _viewModel.stop()
    }

    private fun initializeVideoPlayer() {
        val minBufferDuration = 2000      //Minimum Video you want to buffer while Playing
        val maxBufferDuration = 10000     //Max Video you want to buffer during PlayBack
        val minPlaybackStartBuffer = 100 //Min Video you want to buffer before start Playing it
        val minPlaybackResumeBuffer = 2000   //Min video You want to buffer when user resumes video

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
            player.addListener(_viewModel.videoPlayerEventListener)
            player.volume = if(_viewModel.muteVideoPlayer) 0.0f else 1.0f
            _videoPlayers.add(player)
        }
    }

    private fun releaseVidePlayer() {
        for(player in _videoPlayers) {
            player.release()
        }
        _videoPlayers.clear()
    }

    private fun changePreparingMedia(media: PhotoViewModel.Media?) {
        if(media?.videoUrl != null) {
            val mediaItem = MediaItem.fromUri(media.videoUrl)
            val player = _videoPlayers[media.viewIndex]
            player.setMediaItem(mediaItem)
            player.playWhenReady = true
            player.prepare()
        }
    }

    private fun changeMedia(media: PhotoViewModel.Media?){
        Log.d("PhotoView", "changeMedia media.viewIndex=${media?.viewIndex}")

        val fadeDuration = if(_viewModel.isSlideShow) 1000L else 100 // msec
        val nextView: View = if(media?.bitmap != null){ // next media is bitmap
            _imageViews[media.viewIndex].setImageBitmap(media.bitmap)
            _imageViews[media.viewIndex]
        } else if(media?.videoUrl != null){ // next media is video
            _videoViews[media.viewIndex]
        } else { // next media is blank
            _imageViews[0].setImageResource(R.drawable.blank_image)
            _imageViews[0]
        }

        for(i in _videoViews.indices){
            _videoViews[i].focusable =
                if((media?.videoUrl != null) && (media?.viewIndex == i)) View.FOCUSABLE
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
                    Log.d("PhotoFragment", "onAnimationEnd stop videoPlayer $playerIndex")
                    if(_videoPlayers[playerIndex].isPlaying) {
                        _videoPlayers[playerIndex].stop()
                    }
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
}