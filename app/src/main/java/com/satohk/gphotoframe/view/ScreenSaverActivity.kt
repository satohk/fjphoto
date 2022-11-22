package com.satohk.gphotoframe.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.satohk.gphotoframe.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

import com.satohk.gphotoframe.viewmodel.MainViewModel

/**
 * Loads [PhotoGridFragment].
 */
class ScreenSaverActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val model: MainViewModel by viewModels()

        lifecycleScope.launch {
            model.activeUserName.collect { it ->
                if(it == null){
                    choseAccount()
                }
            }
        }
    }

    private fun choseAccount(){
        val intent = Intent(this, ChooseAccountActivity::class.java)
        startActivity(intent)
    }
}