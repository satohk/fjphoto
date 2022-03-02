package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.AccountState
import com.satohk.gphotoframe.model.Account
import com.satohk.gphotoframe.model.GooglePhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


class MainViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)

    private val _activeUserName = MutableStateFlow<String?>(null)
    val activeUserName: StateFlow<String?> get() = _activeUserName

    init{
        viewModelScope.launch{
            _accountState.activeAccount.collect {
                    account ->
                        if(account != null) {
                            _initPhotoRepository(account)
                            _activeUserName.value = account.userName
                        }
            }
        }
    }

    private fun _initPhotoRepository(account: Account) {
        // google photo only
        val photoRepository = GooglePhotoRepository(account.accessToken)
        _accountState.setPhotoRepository(photoRepository)
    }
}
