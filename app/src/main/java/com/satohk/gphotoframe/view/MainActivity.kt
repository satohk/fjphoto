package com.satohk.gphotoframe.view

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.satohk.gphotoframe.R
import kotlinx.coroutines.launch

import com.satohk.gphotoframe.viewmodel.MainViewModel

/**
 * Loads [PhotoGridFragment].
 */
class MainActivity : FragmentActivity() {
    private val _viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                    _viewModel.activeUserName.collect { it ->
                        if(it == null){
                            Log.d("MainActivity", "activeUserName is null. call choseAccount")
                            chooseAccount()
                        }
                    }
                }
            }
        }
    }

    private fun chooseAccount(){
        val accountTypes = arrayOf(_viewModel.serviceProviderUrl)
        val intent = AccountManager.newChooseAccountIntent(
            null,
            null,
            accountTypes,
            false,
            null,
            null,
            null,
            null
        )

        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!
            val type = data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)!!
            _viewModel.setAccount(type, name, this)
        }
    }
}