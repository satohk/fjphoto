package com.satohk.gphotoframe.view

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.accounts.Account
import android.accounts.AuthenticatorException
import org.koin.java.KoinJavaComponent

import com.satohk.gphotoframe.viewmodel.ChooseAccountViewModel


class ChooseAccountActivity : Activity() {
    private val _viewModel: ChooseAccountViewModel by KoinJavaComponent.inject(ChooseAccountViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chooseAccount()
    }

    private fun chooseAccount(){
        val accountTypes = arrayOf(_viewModel.accountType)
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

    private fun requestToken(name: String, type: String) {
        val account = Account(name, type)
        val manager = AccountManager.get(this)
        val authTokenType = "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly"
        manager.getAuthToken(account, authTokenType, null, this,
            { accountManagerFuture ->
                try {
                    val result = accountManagerFuture.result
                    val token = result.getString(AccountManager.KEY_AUTHTOKEN)
                    this._viewModel.setAccount(name, token!!)
                    finish()
                } catch (e: AuthenticatorException) {
                    e.printStackTrace()
                }
            }, null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode === 101 && resultCode === RESULT_OK) {
            val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)!!
            val type = data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)!!
            requestToken(name, type)
        }
    }
}