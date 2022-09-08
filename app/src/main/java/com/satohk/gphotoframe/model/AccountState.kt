package com.satohk.gphotoframe.model

import com.satohk.gphotoframe.repository.CachedPhotoRepository
import com.satohk.gphotoframe.repository.GooglePhotoRepository
import com.satohk.gphotoframe.repository.TestPhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccountState {
    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> get() = _activeAccount

    private val _photoRepository = MutableStateFlow<CachedPhotoRepository?>(null)
    val photoRepository: StateFlow<CachedPhotoRepository?> get() = _photoRepository

    fun setActiveAccount(account: Account?){
        _activeAccount.value = account

        val repo = this.makePhotoRepository(account)
        _photoRepository.value = repo
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
