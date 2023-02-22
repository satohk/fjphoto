package com.satohk.fjphoto.view

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.AccountPicker
import com.satohk.fjphoto.R
import com.satohk.fjphoto.domain.ServiceProvider
import com.satohk.fjphoto.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Loads [PhotoGridFragment].
 */
class MainActivity : FragmentActivity() {

    private val _viewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch{
                    _viewModel.requestedAccountChange.collect {
                        showSelectAccountDialog()
                    }
                }
            }
        }
    }

    private fun showSelectAccountDialog(){
        Log.d("MainActivity", "requestAccountChange")
        Log.d("MainActivity", "activeUserName is null. call choseAccount")
        val dialogFragment = SelectAccountTypeDialogFragment()
        dialogFragment.show(supportFragmentManager, "dialog")
        dialogFragment.onSelected = fun(accountType: ServiceProvider){
            chooseAccount(accountType)
            dialogFragment.dismiss()
        }
    }

    private fun chooseAccount(accountType: ServiceProvider){
        val accountTypes = listOf(accountType.url)
        val intent = AccountPicker.newChooseAccountIntent(
            AccountPicker.AccountChooserOptions.Builder()
                .setAllowableAccountsTypes(accountTypes)
                .setAlwaysShowAccountPicker(true)
                .build()
        )
        _resultLauncher.launch(intent)
    }

    private val _resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!
            val type = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)!!
            Log.d("MainActivity", "registerForActivityResult name=$name")
            _viewModel.setAccount(type, name, this)
        }
    }
}