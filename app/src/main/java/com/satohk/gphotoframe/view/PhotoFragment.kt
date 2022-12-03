package com.satohk.gphotoframe.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.format.DateTimeFormatter


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoFragment() : Fragment(R.layout.fragment_photo) {
    private val _viewModel by viewModel<PhotoViewModel>()
    private val _imageViews = mutableListOf<ImageView>()
    private val _videoViews = mutableListOf<StyledPlayerView>()
    private val _videoPlayers = mutableListOf<ExoPlayer>()
    private var _currentMediaView: View? = null
    private var _slideshowMode: Boolean = true
    private var _startIndex: Int = 0
    private var _contents: GridContents? = null  // If null, use the value set in Setting

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(arguments != null) {
            _slideshowMode = arguments!!.getBoolean("slideShow")
            _startIndex = arguments!!.getInt("showIndex")
            _contents = arguments!!.get("contents") as GridContents?
        }

        Log.d("PhotoFragment", _slideshowMode.toString())
        Log.d("PhotoFragment", _startIndex.toString())
        Log.d("PhotoFragment", _contents.toString())

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

        dummyButton.setOnFocusChangeListener { view, b ->
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
                    _viewModel.errorMessageId.collect {
                        it?.let { messageId ->
                            Toast.makeText(context, getText(messageId), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializeVideoPlayer()
        _viewModel.onStart(_contents, _slideshowMode, _startIndex)
    }

    override fun onStop() {
        super.onStop()
        releaseVidePlayer()
        _viewModel.onStop()
    }

    private fun initializeVideoPlayer() {
        val MIN_BUFFER_DURATION = 2000      //Minimum Video you want to buffer while Playing
        val MAX_BUFFER_DURATION = 10000     //Max Video you want to buffer during PlayBack
        val MIN_PLAYBACK_START_BUFFER = 100 //Min Video you want to buffer before start Playing it
        val MIN_PLAYBACK_RESUME_BUFFER = 2000   //Min video You want to buffer when user resumes video

        for(videoView in _videoViews) {
            val loadControl: LoadControl = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, 16))
                .setBufferDurationsMs(
                    MIN_BUFFER_DURATION,
                    MAX_BUFFER_DURATION,
                    MIN_PLAYBACK_START_BUFFER,
                    MIN_PLAYBACK_RESUME_BUFFER
                )
                .setTargetBufferBytes(-1)
                .setPrioritizeTimeOverSizeThresholds(true).createDefaultLoadControl()

            val trackSelector: TrackSelector = DefaultTrackSelector(this.context!!)

            val player = ExoPlayer.Builder(this.context!!)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build()
                .also { exoPlayer ->
                    videoView.player = exoPlayer
                }
            _videoPlayers.add(player)
        }
        _viewModel.setVidePlayers(_videoPlayers)
    }

    private fun releaseVidePlayer() {
        for(player in _videoPlayers) {
            player.release()
        }
        _videoPlayers.clear()
    }

    private fun changeMedia(media: PhotoViewModel.Media){
        Log.d("changeMedia", media.toString())

        val fadeDuration = if(_slideshowMode) 1500L else 100 // msec
        var nextView: View? = null

        if(media.bitmap != null){ // next media is bitmap
            for(imageView in _imageViews){
                if(imageView != _currentMediaView){
                    imageView.setImageBitmap(media.bitmap!!)
                    nextView = imageView
                }
            }
        }
        else{ // next media is video
            for(videoView in _videoViews){
                if(videoView.player == media.videoPlayer){
                    nextView = videoView
                }
            }
        }
        if(nextView == null){ // next media is blank
            nextView = _imageViews[0]
            nextView.setImageResource(R.drawable.blank_image)
        }

        for(videoView in _videoViews){
            videoView.focusable = if(videoView.player == media.videoPlayer) View.FOCUSABLE else View.NOT_FOCUSABLE
        }

        nextView.alpha = 0.0f
        nextView.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(nextView, "alpha", 1.0f).apply {
            duration = fadeDuration
            start()
        }
        ObjectAnimator.ofFloat(_currentMediaView, "alpha", 0.0f).apply {
            duration = fadeDuration
            start()
        }

        _viewModel.currentPhotoMetadata?.let {
            photoInfo.text =
                it.metadataRemote.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        _currentMediaView = nextView
    }
}