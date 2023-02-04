package com.satohk.fjphoto.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satohk.fjphoto.R
import com.satohk.fjphoto.viewmodel.PhotoViewModel
import com.satohk.fjphoto.viewmodel.ScreenSaverViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class ScreenSaverActivity : FragmentActivity() {
    private val _viewModel: ScreenSaverViewModel by viewModel<ScreenSaverViewModel>()
    private val _photoViewModel: PhotoViewModel by viewModel<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ScreenSaverActivity", "onCreate")
        setContentView(R.layout.activity_screen_saver)
        _viewModel.setActivity(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    _viewModel.slideShowContent.collect { it ->
                        if (it != null) {
                            Log.d("ScreenSaverActivity", "slideShowContent changed: ${it}")
                            _photoViewModel.gridContents = it
                            _photoViewModel.start()
                        }
                    }
                }
            }
        }
    }
}