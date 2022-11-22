package com.satohk.gphotoframe.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.domain.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


class MainViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)

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
