package com.satohk.gphotoframe.model

import com.satohk.gphotoframe.repository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.GooglePhotoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

class AccountState {
    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> get() = _activeAccount

    private val _photoRepository = MutableStateFlow<CachedPhotoRepository?>(null)
    val photoRepository: StateFlow<CachedPhotoRepository?> get() = _photoRepository

    var photoMetadataStore:PhotoMetadataStore? = null
        private set

    fun setActiveAccount(account: Account?){
        _activeAccount.value = account

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

        photoMetadataStore?.let{
            it.saveToLocalFile()
        }
        photoMetadataStore = PhotoMetadataStore(account.serviceProvider.toString())

        return CachedPhotoRepository(repo)
    }
}
