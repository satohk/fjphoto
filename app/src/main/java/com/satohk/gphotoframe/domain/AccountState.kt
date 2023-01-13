package com.satohk.gphotoframe.domain

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.satohk.gphotoframe.R
import com.satohk.gphotoframe.repository.localrepository.SettingRepository
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.remoterepository.GooglePhotoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.java.KoinJavaComponent.inject

class AccountState(private val _context: Context) {
    private val _activeAccount = MutableStateFlow<AccountWithToken?>(null)
    val activeAccount: StateFlow<AccountWithToken?> get() = _activeAccount

    private val _photoRepository = MutableStateFlow<CachedPhotoRepository?>(null)
    val photoRepository: StateFlow<CachedPhotoRepository?> get() = _photoRepository

    val settingRepository: SettingRepository by inject(SettingRepository::class.java)

    fun requestToken(providerUrl:String, userName:String, activity: Activity){
        Log.d("setActiveAccount", "accountType='$providerUrl', username='$userName'")
        val account = Account(userName, providerUrl)
        val manager = AccountManager.get(_context)
        val authTokenType = "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly"
        manager.getAuthToken(account, authTokenType, null, activity,
            { accountManagerFuture ->
                try {
                    val result = accountManagerFuture.result
                    val token = result.getString(AccountManager.KEY_AUTHTOKEN)
                    setAccount(AccountWithToken(providerUrl, userName, token!!))
                } catch (e: AuthenticatorException) {
                    Log.d("getAuthToken callback", "exception:${e.message}")
                    Log.d("getAuthToken callback", "exception:${e}")
                    e.printStackTrace()
                }
            }, null)
    }

    fun setAccount(account:AccountWithToken?){
        _activeAccount.value = account

        val repo = this.makePhotoRepository(account)
        _photoRepository.value = repo
        Log.d("setActiveAccount", "_photoRepository.value=${_photoRepository.value}")

        repo?.let { it ->
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                it.lastError.collect { error ->
                    if(_photoRepository.value != null) {
                        Log.d("AccountState", "errorOccured. error=$error, current account ${_activeAccount.value}")
                        if(error == CachedPhotoRepository.ErrorType.ERR_COMMUNICATION){
                            _activeAccount.value?.let {
                                Toast.makeText(_context, _context.getText(R.string.msg_network_error), Toast.LENGTH_LONG).show()
                                val manager = AccountManager.get(_context)
                                manager.invalidateAuthToken(it.serviceProviderUrl, it.accessToken)
                            }
                            _activeAccount.value = null
                            _photoRepository.value = null
                            scope.cancel()
                        }
                        else if(error == CachedPhotoRepository.ErrorType.ERR_TIMEOUT){
                            Toast.makeText(_context, _context.getText(R.string.msg_timeout_error), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

    }

    private fun makePhotoRepository(account: AccountWithToken?): CachedPhotoRepository? {
        if (account == null) {
            return null
        }
        val repo = when (account.serviceProviderUrl) {
            ServiceProvider.GOOGLE.url -> GooglePhotoRepository(account.accessToken)
            //ServiceProvider.GOOGLE -> TestPhotoRepository(account.accessToken)
            else -> GooglePhotoRepository(account.accessToken)
        }

        return CachedPhotoRepository(repo)
    }
}
