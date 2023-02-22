package com.satohk.fjphoto.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainViewModel(private val _accountState: AccountState) : ViewModel() {
    private val _requestedAccountChange = MutableSharedFlow<Boolean>(replay=1)
    val requestedAccountChange: SharedFlow<Boolean> get() = _requestedAccountChange

    init{
        viewModelScope.launch{
            _accountState.requestedAccountChange.collect {
                _requestedAccountChange.emit(true)
            }
        }
        viewModelScope.launch {
            _requestedAccountChange.emit(true)
        }
    }

    fun setAccount(providerUrl:String, username:String, activity: Activity){
        _accountState.requestToken(providerUrl, username, activity)
    }
}
