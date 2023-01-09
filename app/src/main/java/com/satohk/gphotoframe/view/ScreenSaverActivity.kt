package com.satohk.gphotoframe.view

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.viewmodel.MainViewModel


class ScreenSaverActivity : FragmentActivity() {
    private val _viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ScreenSaverActivity", "onCreate")
        setContentView(R.layout.activity_screen_saver)
    }
}