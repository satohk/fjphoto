package com.satohk.gphotoframe.model

import com.satohk.gphotoframe.repository.CachedPhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Setting {

    private val _activeAccount = MutableStateFlow<Account?>(null)
    val activeAccount: StateFlow<Account?> get() = _activeAccount

    private val _photoRepository = MutableStateFlow<CachedPhotoRepository?>(null)
    val photoRepository: StateFlow<CachedPhotoRepository?> get() = _photoRepository
}