package com.satohk.gphotoframe.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.*
import androidx.navigation.fragment.navArgs
import com.satohk.gphotoframe.*

/**
 * Loads a grid of cards with movies to browse.
 */
class PhotoFragment() : Fragment(R.layout.fragment_photo) {
    //private val _viewModel by sharedViewModel<PhotoGridWithSideBarViewModel>()
    private val args: PhotoFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("PhotoFragment", args.slideShow.toString())
        Log.d("PhotoFragment", args.showIndex.toString())
        Log.d("PhotoFragment", args.contents.toString())
    }
}