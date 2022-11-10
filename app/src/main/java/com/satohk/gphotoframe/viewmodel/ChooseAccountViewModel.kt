package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.domain.Account
import com.satohk.gphotoframe.domain.ServiceProvider


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