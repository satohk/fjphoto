package com.satohk.fjphoto.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainViewModel(private val _accountState: AccountState) : ViewModel() {
    private val _requestedAccountChange = MutableSharedFlow<Boolean>()
    val requestedAccountChange: SharedFlow<Boolean> get() = _requestedAccountChange

    init{
        viewModelScope.launch{
            _accountState.requestedAccountChange.collect {
                Log.d("MainViewModel", "_requestedAccountChange.emit")
                _requestedAccountChange.emit(true)
            }
        }
        viewModelScope.launch {
            _requestedAccountChange.emit(true)
        }
        viewModelScope.launch {
            _accountState.photoMetadataSyncer.collect{
                it?.syncAll()
            }
        }
    }

    fun setAccount(providerUrl:String, username:String, activity: Activity){
        Log.d("MainViewModel", "setAccount")
        _accountState.requestToken(providerUrl, username, activity)
    }
}
