package com.satohk.fjphoto.domain

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.satohk.fjphoto.R
import com.satohk.fjphoto.repository.localrepository.AppDatabase
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataRemoteCacheRepository
import com.satohk.fjphoto.repository.localrepository.SettingRepository
import com.satohk.fjphoto.repository.remoterepository.GooglePhotoRepository
import com.satohk.fjphoto.repository.remoterepository.PhotoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.java.KoinJavaComponent.inject

class AccountState(private val _context: Context) {
    private val _activeAccount = MutableStateFlow<AccountWithToken?>(null)
    val activeAccount: StateFlow<AccountWithToken?> get() = _activeAccount
    private val _requestedAccountChange = MutableSharedFlow<Boolean>()
    val requestedAccountChange: SharedFlow<Boolean> get() = _requestedAccountChange

    private val _photoLoader = MutableStateFlow<CachedPhotoLoader?>(null)
    val photoLoader: StateFlow<CachedPhotoLoader?> get() = _photoLoader

    private var _photoMetadataSyncer = MutableStateFlow<PhotoMetadataSyncer?>(null)
    val photoMetadataSyncer: StateFlow<PhotoMetadataSyncer?> get() = _photoMetadataSyncer

    val settingRepository: SettingRepository by inject(SettingRepository::class.java)
    val photoMetadataRemoteCacheRepository: PhotoMetadataRemoteCacheRepository by inject(PhotoMetadataRemoteCacheRepository::class.java)

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

    private fun setAccount(account:AccountWithToken?) {
        _activeAccount.value = account

        val photoRepo = this.makePhotoRepository(account)
        _photoLoader.value = if (photoRepo != null && account != null) CachedPhotoLoader(
            photoRepo, account.accountId) else null
        val photoRepo2 = this.makePhotoRepository(account)
        _photoMetadataSyncer.value = if (photoRepo2 != null && account != null) PhotoMetadataSyncer(
            photoRepo2,
            photoMetadataRemoteCacheRepository,
            account.accountId
        ) else null

        Log.d("setActiveAccount", "_photoRepository.value=${_photoLoader.value}")

        _photoLoader.value?.let { it ->
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                it.lastError.collect { error ->
                    if (_photoLoader.value != null) {
                        Log.d(
                            "AccountState",
                            "errorOccured. error=$error, current account ${_activeAccount.value?.userName}"
                        )
                        when (error) {
                            CachedPhotoLoader.ErrorType.ERR_COMMUNICATION -> {
                                _activeAccount.value?.let {
                                    Toast.makeText(
                                        _context,
                                        _context.getText(R.string.msg_network_error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val manager = AccountManager.get(_context)
                                    manager.invalidateAuthToken(
                                        it.serviceProviderUrl,
                                        it.accessToken
                                    )
                                }
                                _activeAccount.value = null
                                _photoLoader.value = null
                                _photoMetadataSyncer.value = null
                                _requestedAccountChange.emit(true)
                                scope.cancel()
                            }

                            CachedPhotoLoader.ErrorType.ERR_TIMEOUT -> {
                                Toast.makeText(
                                    _context,
                                    _context.getText(R.string.msg_timeout_error),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            CachedPhotoLoader.ErrorType.ERR_DISCONNECTED -> {
                                Toast.makeText(
                                    _context,
                                    _context.getText(R.string.msg_disconnected),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
        _photoMetadataSyncer.value?.let { syncer->
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                syncer.synced.collect { synced ->
                    if(synced){
                        Toast.makeText(
                            _context,
                            _context.getText(R.string.msg_synced),
                            Toast.LENGTH_LONG
                        ).show()
                        if(_photoLoader.value != null) {
                            _photoLoader.value!!.photoMetadataRemoteCacheRepository = photoMetadataRemoteCacheRepository
                        }
                    }
                }
            }
        }
    }

    suspend fun changeAccount(){
        Log.d("AccountState", "changeAccount")
        _requestedAccountChange.emit(true)
    }

    private fun makePhotoRepository(account: AccountWithToken?): PhotoRepository? {
        if (account == null) {
            return null
        }
        val repo = when (account.serviceProviderUrl) {
            ServiceProvider.GOOGLE.url -> GooglePhotoRepository(account.accessToken)
            //ServiceProvider.GOOGLE -> TestPhotoRepository(account.accessToken)
            else -> GooglePhotoRepository(account.accessToken)
        }

        return repo
    }
}
