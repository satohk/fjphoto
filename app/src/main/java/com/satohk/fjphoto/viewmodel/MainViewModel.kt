package com.satohk.fjphoto.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.fjphoto.domain.AccountState
import com.satohk.fjphoto.domain.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


class MainViewModel(private val _accountState: AccountState) : ViewModel() {
    private val _activeUserName = MutableStateFlow<String?>(null)
    val activeUserName: StateFlow<String?> get() = _activeUserName
    private var _serviceProvider: ServiceProvider = ServiceProvider.GOOGLE
    val serviceProviderUrl = _serviceProvider.url

    init{
        viewModelScope.launch{
            _accountState.activeAccount.collect { account ->
                if(account != null) {
                    _activeUserName.value = account.userName
                }
                else{
                    _activeUserName.value = null
                }
            }
        }
    }

    fun setAccount(providerUrl:String, username:String, activity: Activity){
        _accountState.requestToken(providerUrl, username, activity)
    }
}
