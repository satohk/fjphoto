package com.satohk.gphotoframe.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.satohk.gphotoframe.*
import com.satohk.gphotoframe.viewmodel.PhotoViewModel
import kotlinx.android.synthetic.main.fragment_photo.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoFragment() : Fragment(R.layout.fragment_photo) {
    //private val _viewModel by sharedViewModel<PhotoGridWithSideBarViewModel>()
    private val args: PhotoFragmentArgs by navArgs()
    private val _viewModel by viewModel<PhotoViewModel>()
    private lateinit var showImageView: ImageView
    private lateinit var hideImageView: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("PhotoFragment", args.slideShow.toString())
        Log.d("PhotoFragment", args.showIndex.toString())
        Log.d("PhotoFragment", args.contents.toString())

        showImageView = view.findViewById<ImageView>(R.id.imageView1)
        hideImageView = view.findViewById<ImageView>(R.id.imageView2)
        hideImageView.alpha = 0.0f
        showImageView.setImageResource(R.drawable.blank_image)

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
                changeImage(it)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onStart() {
        super.onStart()
        _viewModel.onStart(args.contents, args.slideShow, args.showIndex)
    }

    override fun onStop() {
        super.onStop()
        _viewModel.onStop()
    }

    private fun changeImage(bmp: Bitmap){
        val fadeDuration = if(args.slideShow) 1500L else 0L // msec

        hideImageView.setImageBitmap(bmp)
        ObjectAnimator.ofFloat(hideImageView, "alpha", 1.0f).apply {
            duration = fadeDuration
            start()
        }
        ObjectAnimator.ofFloat(showImageView, "alpha", 0.0f).apply {
            duration = fadeDuration
            start()
        }

        photoInfo.text = _viewModel.currentPhotoMetadata!!.timestamp.toString()

        val tmp = hideImageView
        hideImageView = showImageView
        showImageView = tmp
    }
}