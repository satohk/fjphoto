package com.satohk.gphotoframe.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.AccountState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ScreenSaverViewModel(private val _accountState: AccountState) : ViewModel() {
    private var _activity: Activity? = null
    private val _slideShowContent = MutableStateFlow<GridContents?>(null)
    val slideShowContent: StateFlow<GridContents?> get() = _slideShowContent

    init{
        viewModelScope.launch{
            Log.d("ScreenSaverViewModel", "init")
            _accountState.settingRepository.setting.collect {
                Log.d("ScreenSaverViewModel", "_accountState.settingRepository.setting.collect")
                setAccount()
                _slideShowContent.value = GridContents(it.screensaverSearchQuery)
            }
        }
    }

    fun setActivity(activity: Activity){
        Log.d("ScreenSaverViewModel", "setActivity: ${activity}")
        _activity = activity
        setAccount()
    }

    private fun setAccount(){
        Log.d("ScreenSaverViewModel", "setAccount setting.value=${_accountState.settingRepository.setting.value} activity=${_activity}")
        _accountState.settingRepository.setting.value?.let {
            if(it.screensaverSearchQuery.userName != null && it.screensaverSearchQuery.serviceProviderUrl != null && _activity != null) {
                Log.i("requestToken", "userName:${it.screensaverSearchQuery.userName}, provider=${it.screensaverSearchQuery.serviceProviderUrl}")
                _accountState.requestToken(it.screensaverSearchQuery.serviceProviderUrl, it.screensaverSearchQuery.userName, _activity!!)
            }
        }
    }
}
