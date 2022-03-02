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
    }

    fun setPhotoRepository(repo: PhotoRepository?){
        _photoRepository.value = repo
    }
}
