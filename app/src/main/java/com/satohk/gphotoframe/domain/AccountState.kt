package com.satohk.gphotoframe.domain

import com.satohk.gphotoframe.repository.localrepository.PhotoMetadataStore
import com.satohk.gphotoframe.repository.localrepository.SettingRepository
import com.satohk.gphotoframe.repository.remoterepository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.remoterepository.GooglePhotoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import org.koin.java.KoinJavaComponent.inject

class AccountState {
    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> get() = _activeAccount

    private val _photoRepository = MutableStateFlow<CachedPhotoRepository?>(null)
    val photoRepository: StateFlow<CachedPhotoRepository?> get() = _photoRepository

    val settingRepository: SettingRepository by inject(SettingRepository::class.java)

    private val _scope = CoroutineScope(Job() + Dispatchers.IO)

    fun setActiveAccount(account: Account?){
        _activeAccount.value = account

        account?.let {
            _scope.launch {
                settingRepository.load(it.userName)
            }
        }

        val repo = this.makePhotoRepository(account)
        _photoRepository.value = repo

        repo?.let { it ->
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                it.errorOccured.collect { error ->
                    if(error) {
                        _activeAccount.value = null
                        _photoRepository.value = null
                        scope.cancel()
                    }
                }
            }
        }
    }

    private fun makePhotoRepository(account: Account?): CachedPhotoRepository? {
        if (account == null) {
            return null
        }
        val repo = when (account.serviceProvider) {
            ServiceProvider.GOOGLE -> GooglePhotoRepository(account.accessToken)
            //ServiceProvider.GOOGLE -> TestPhotoRepository(account.accessToken)
        }

        return CachedPhotoRepository(repo)
    }
}
