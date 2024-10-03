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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.*
import androidx.media3.common.*
import com.satohk.fjphoto.R
import com.satohk.fjphoto.viewmodel.GridContents
import com.satohk.fjphoto.viewmodel.PhotoViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoFragment() : Fragment(R.layout.fragment_photo) {
    private val _viewModel by sharedViewModel<PhotoViewModel>()
    private val _imageViews = mutableListOf<ImageView>()
    private lateinit var _videoView: PlayerView
    private lateinit var _photoInfo: TextView
    private var _videoPlayer: ExoPlayer? = null
    private var _videoPlayerListener: VideoPlayerListener? = null
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
        _imageViews.clear()
        _imageViews.add(view.findViewById(R.id.imageView1))
        _imageViews.add(view.findViewById(R.id.imageView2))
        _videoView = view.findViewById(R.id.videoView)
        _photoInfo = view.findViewById(R.id.photoInfo)
        for(imageView in _imageViews){
            imageView.setImageResource(R.drawable.blank_image)
            imageView.visibility = View.INVISIBLE
            imageView.alpha = 0.0f
        }
        _videoView.controllerAutoShow = false
        _videoView.visibility = View.INVISIBLE
        _videoView.alpha = 0.0f

        // focusをコントロールするためのダミーボタン
        val dummyButton = view.findViewById<Button>(R.id.dummyButtonTop)
        dummyButton.setOnKeyListener { _: View, _: Int, keyEvent: KeyEvent ->
            Log.d("PhotoFragment", "KeyEvent " + keyEvent.toString())
            if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                if(_currentMediaView is PlayerView){
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> (_currentMediaView as PlayerView).showController()
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
                if(_currentMediaView is PlayerView){
                    (_currentMediaView as PlayerView).hideController()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                    _viewModel.prepareMedia.collect {
                        Log.d("PhotoFragment", "_viewModel.prepareVideo.collect")
                        prepareVideo(it)
                    }
                }
                launch{
                    _viewModel.currentMedia.collect {
                        Log.d("PhotoFragment", "_viewModel.currentMedia.collect")
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
    }

    override fun onResume(){
        Log.d("PhotoFragment", "onResume")
        super.onResume()
        setPhotoSize()
        initializeVideoPlayer()
        _viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        Log.d("PhotoFragment", "onPause")
        releaseVidePlayer()
        _viewModel.stop()
    }

    private fun initializeVideoPlayer() {
        _videoPlayer = ExoPlayer.Builder(this.requireContext())
            .build()
            .also { exoPlayer ->
                _videoView.player = exoPlayer
            }
        _videoPlayerListener = VideoPlayerListener(_viewModel)
        _videoPlayerListener!!.ownerPlayer = _videoPlayer
        _videoPlayer!!.addListener(_videoPlayerListener!!)
        _videoPlayer!!.volume = if(_viewModel.muteVideoPlayer) 0.0f else 1.0f
    }

    private fun releaseVidePlayer() {
        Log.d("PhotoFragment", "releaseVideoPlayers videoPlayer=$_videoPlayer")
        _videoPlayer?.let{
            it.playWhenReady = false
            it.clearVideoSurface()
            it.clearMediaItems()
            it.stop()
            it.release()
            Log.d("PhotoFragment", "releaseVideoPlayers videoPlayer=$_videoPlayer released")
        }
        _videoPlayer = null
        //videoView.player = null
    }

    private fun prepareVideo(media: PhotoViewModel.Media) {
        Log.d("PhotoFragment", "prepareVideo media=${media.index}")
        if(media.videoUrl != null) {
            val mediaItem = MediaItem.fromUri(media.videoUrl)
            _videoPlayerListener?.mediaIndex = media.index
            _videoPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.setMediaItem(mediaItem)
                it.playWhenReady = true
                it.prepare()
            }
        }
    }

    private fun nextImageView(): ImageView{
        val nextImageViewIndex = if(_currentMediaView == _imageViews[0]) 1 else 0
        Log.d("PhotoView", "nextImageView nextIndex=${nextImageViewIndex}")

        return _imageViews[nextImageViewIndex]
    }

    private fun changeMedia(media: PhotoViewModel.Media) {
        val nextView: View = if (media.photo != null) {
            Log.d("PhotoView", "changeMedia photo mediaIndex=${media.index}")
            _videoView.focusable = View.NOT_FOCUSABLE
            val v = nextImageView()
            v.setImageBitmap(media.photo)
            v
        } else if (media.videoUrl != null) {
            Log.d("PhotoView", "changeMedia video mediaIndex=${media.index}")
            _videoView.focusable = View.FOCUSABLE
            _videoView
        } else { // next media is blank
            Log.d("PhotoView", "changeMedia blank mediaIndex=${media.index}")
            val v = nextImageView()
            v.setImageResource(R.drawable.blank_image)
            v
        }

        val prevView =_currentMediaView
        if(prevView == _videoView){
            _videoPlayer?.stop()
        }

        if(media.fadeInDuration == 0){
            _currentMediaView?.visibility = View.INVISIBLE
            _currentMediaView?.alpha = 0.0f
            nextView.visibility = View.VISIBLE
            nextView.alpha = 1.0f
        }
        else {
            nextView.alpha = 0.0f
            nextView.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(nextView, "alpha", 1.0f).apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        Log.d("PhotoView", "fadedIn NextView mediaIndex=${media.index}")
                        _viewModel.onMediaStarted(media.index)
                    }
                })
                duration = media.fadeInDuration.toLong()
                start()
            }

            if (_currentMediaView != null) {
                ObjectAnimator.ofFloat(prevView!!, "alpha", 0.0f).apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            Log.d("PhotoView", "fadedOut PrevView")
                            prevView?.visibility = View.INVISIBLE
                        }
                    })
                    duration = media.fadeInDuration.toLong()
                    start()
                }
            }
        }

        media.let {
            _photoInfo.text = media.info
        }

        _currentMediaView = nextView
    }

    private fun setPhotoSize(){
        _viewModel.photoWidth = _imageViews[0].width
        _viewModel.photoHeight = _imageViews[0].height
        Log.d("PhotoFragment", "setPhotoSize w:${_viewModel.photoWidth} h:${_viewModel.photoHeight}")
    }

    private class VideoPlayerListener(private val _viewModel:PhotoViewModel) : Player.Listener{
        var mediaIndex: Int = 0
        var ownerPlayer: ExoPlayer? = null

        override fun onPlayerError(error: PlaybackException) {
            Log.d("PhotoFragment", "onPlayerError error=$error owner=$ownerPlayer")
            _viewModel.onPlayerError(error, mediaIndex)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d("PhotoFragment", "onPlaybackStateChanged playbackState=$playbackState owner=$ownerPlayer")
            _viewModel.onPlaybackStateChanged(playbackState, mediaIndex)
        }
    }
}