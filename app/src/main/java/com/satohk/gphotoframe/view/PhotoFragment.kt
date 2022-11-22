package com.satohk.gphotoframe.view

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.GridContents
import com.satohk.gphotoframe.viewmodel.PhotoViewModel
import kotlinx.android.synthetic.main.fragment_photo.*
import kotlinx.android.synthetic.main.fragment_photo.view.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.format.DateTimeFormatter


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoFragment() : Fragment(R.layout.fragment_photo) {
    private val _viewModel by viewModel<PhotoViewModel>()
    private lateinit var _showImageView: ImageView
    private lateinit var _hideImageView: ImageView
    private lateinit var _videoView: StyledPlayerView
    private var _player: ExoPlayer? = null
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

        _showImageView = view.findViewById(R.id.imageView1)
        _hideImageView = view.findViewById(R.id.imageView2)
        _videoView = view.findViewById(R.id.videoView)
        _videoView.controllerAutoShow = false

        _hideImageView.alpha = 0.0f
        _showImageView.setImageResource(R.drawable.blank_image)

        // focusをコントロールするためのダミーボタン
        val dummyButton = view.findViewById<Button>(R.id.dummyButtonTop)
        dummyButton.setOnKeyListener { _: View, _: Int, keyEvent: KeyEvent ->
            Log.d("PhotoFragment", "KeyEvent " + keyEvent.toString())
            if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                if(_videoView.visibility != View.VISIBLE) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> _viewModel.goPrev()
                        KeyEvent.KEYCODE_DPAD_RIGHT -> _viewModel.goNext()
                        else -> return@setOnKeyListener false
                    }
                    return@setOnKeyListener true
                }
                else{
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> _videoView.showController()
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
            Log.d("PhotoFragment", "dummyButton focus " + b.toString())
            if(b) {
                if(_videoView.visibility == View.VISIBLE){
                    _videoView.hideController()
                }
            }
        }

        _viewModel.currentPhoto.onEach{
            if(it != null) {
                _videoView.visibility = View.INVISIBLE
                _showImageView.visibility = View.VISIBLE
                _hideImageView.visibility = View.VISIBLE
                changeImage(it)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        _viewModel.currentVideoRequest.onEach{ it ->
            if(it != null) {
                _videoView.visibility = View.VISIBLE
                _videoView.requestFocus()
                _showImageView.visibility = View.INVISIBLE
                _hideImageView.visibility = View.INVISIBLE
                Log.d("mediaItem", it.url)
                val mediaItem = MediaItem.fromUri(it.url)
                _player?.let{
                    it.setMediaItem(mediaItem)
                    it.playWhenReady = true
                    it.prepare()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
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
        //Minimum Video you want to buffer while Playing
        val MIN_BUFFER_DURATION = 2000
        //Max Video you want to buffer during PlayBack
        val MAX_BUFFER_DURATION = 10000
        //Min Video you want to buffer before start Playing it
        val MIN_PLAYBACK_START_BUFFER = 100
        //Min video You want to buffer when user resumes video
        val MIN_PLAYBACK_RESUME_BUFFER = 2000

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

        _player = ExoPlayer.Builder(this.context!!)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .also { exoPlayer ->
                _videoView.videoView.player = exoPlayer
            }
    }

    private fun releaseVidePlayer() {
        _player?.release()
        _player = null
    }

    private fun changeImage(bmp: Bitmap){
        val fadeDuration = if(_slideshowMode) 1500L else 0L // msec

        _hideImageView.setImageBitmap(bmp)
        ObjectAnimator.ofFloat(_hideImageView, "alpha", 1.0f).apply {
            duration = fadeDuration
            start()
        }
        ObjectAnimator.ofFloat(_showImageView, "alpha", 0.0f).apply {
            duration = fadeDuration
            start()
        }

        _viewModel.currentPhotoMetadata?.let {
            photoInfo.text = it.metadataRemote.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        val tmp = _hideImageView
        _hideImageView = _showImageView
        _showImageView = tmp
    }
}