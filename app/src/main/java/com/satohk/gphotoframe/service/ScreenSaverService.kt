package com.satohk.gphotoframe.service

import android.content.Intent
import android.service.dreams.DreamService
import android.util.Log
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.view.ScreenSaverActivity


class ScreenSaverService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("ScreenSaverService", "onAttachedToWindow")
        isInteractive = false
        isFullscreen = true
        val intent = Intent(this, ScreenSaverActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
//        setContentView(R.layout.activity_screen_saver)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d("ScreenSaverService", "onDreamingStarted")
        finish()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.d("ScreenSaverService", "onDreamingStopped")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d("ScreenSaverService", "onDetachedFromWindow")
    }
}