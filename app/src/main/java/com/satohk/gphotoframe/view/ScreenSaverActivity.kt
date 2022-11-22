package com.satohk.gphotoframe.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.satohk.gphotoframe.R


class ScreenSaverActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ScreenSaverActivity", "onCreate")
        setContentView(R.layout.activity_screen_saver)
    }
}