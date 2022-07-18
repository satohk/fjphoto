package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import com.satohk.gphotoframe.model.AccountState
import com.satohk.gphotoframe.model.Account
import com.satohk.gphotoframe.model.ServiceProvider
import org.koin.java.KoinJavaComponent.inject


class ChooseAccountViewModel(
    private val _accountState: AccountState
)  : ViewModel() {
    private var _serviceProvider: ServiceProvider = ServiceProvider.GOOGLE
    val accountType: String
        get() = _serviceProvider.url

    fun setAccount(username:String, accessToken:String){
        _accountState.setActiveAccount(Account(ServiceProvider.GOOGLE, username, accessToken))
    }
}