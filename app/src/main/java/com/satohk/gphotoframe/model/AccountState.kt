package com.satohk.gphotoframe.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccountState {
    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> get() = _activeAccount

    private val _photoRepository = MutableStateFlow<PhotoRepository?>(null)
    val photoRepository: StateFlow<PhotoRepository?> get() = _photoRepository

    fun setActiveAccount(account: Account?){
        _activeAccount.value = account

        val repo = this.makePhotoRepository(account)
        _photoRepository.value = repo
    }

    fun makePhotoRepository(account:Account?): PhotoRepository?{
        if(account == null){
            return null
        }
        val repo = when(account.serviceProvider){
            ServiceProvider.GOOGLE -> GooglePhotoRepository(account.accessToken)
        }
        val cachedRepo = CachedPhotoRepository(repo)

        return cachedRepo
    }
}
