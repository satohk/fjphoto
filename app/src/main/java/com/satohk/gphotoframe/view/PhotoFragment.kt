package com.satohk.gphotoframe.view

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.Visibility
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.satohk.gphotoframe.R
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
    //private val _viewModel by sharedViewModel<PhotoGridWithSideBarViewModel>()
    private val _args: PhotoFragmentArgs by navArgs()
    private val _viewModel by viewModel<PhotoViewModel>()
    private lateinit var _showImageView: ImageView
    private lateinit var _hideImageView: ImageView
    private lateinit var _videoView: StyledPlayerView
    private var _player: ExoPlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("PhotoFragment", _args.slideShow.toString())
        Log.d("PhotoFragment", _args.showIndex.toString())
        Log.d("PhotoFragment", _args.contents.toString())

        _showImageView = view.findViewById(R.id.imageView1)
        _hideImageView = view.findViewById(R.id.imageView2)
        _videoView = view.findViewById(R.id.videoView)
        _hideImageView.alpha = 0.0f
        _showImageView.setImageResource(R.drawable.blank_image)

        view.focusable = View.FOCUSABLE
        view.isFocusableInTouchMode = true;
        view.setOnKeyListener { _: View, _: Int, keyEvent: KeyEvent ->
            Log.d("PhotoFragment", "KeyEvent " + keyEvent.toString())
            if(keyEvent.action == KeyEvent.ACTION_DOWN) {
                when(keyEvent.keyCode){
                    KeyEvent.KEYCODE_DPAD_LEFT -> _viewModel.goPrev()
                    KeyEvent.KEYCODE_DPAD_RIGHT -> _viewModel.goNext()
                    else -> return@setOnKeyListener false
                }
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        _viewModel.currentPhoto.onEach{
            if(it != null) {
                _videoView.visibility = View.INVISIBLE
                changeImage(it)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        _viewModel.currentVideoRequest.onEach{ it ->
            if(it != null) {
                _videoView.visibility = View.VISIBLE
                val mediaItem = MediaItem.fromUri(it.url)
                _player?.setMediaItem(mediaItem)
                _player?.playWhenReady = true
                _player?.prepare()
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onStart() {
        super.onStart()
        initializeVideoPlayer()
        _viewModel.onStart(_args.contents, _args.slideShow, _args.showIndex)
    }

    override fun onStop() {
        super.onStop()
        releaseVidePlayer()
        _viewModel.onStop()
    }

    private fun initializeVideoPlayer() {
        _player = ExoPlayer.Builder(this.context!!)
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
        val fadeDuration = if(_args.slideShow) 1500L else 0L // msec

        _hideImageView.setImageBitmap(bmp)
        ObjectAnimator.ofFloat(_hideImageView, "alpha", 1.0f).apply {
            duration = fadeDuration
            start()
        }
        ObjectAnimator.ofFloat(_showImageView, "alpha", 0.0f).apply {
            duration = fadeDuration
            start()
        }

        photoInfo.text = _viewModel.currentPhotoMetadata!!.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val tmp = _hideImageView
        _hideImageView = _showImageView
        _showImageView = tmp
    }
}