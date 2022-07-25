package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


// Abstract class
open class SideBarActionPublisherViewModel : ViewModel() {
    private val _action = MutableSharedFlow<SideBarAction>()
    val action = _action.asSharedFlow()

    protected fun publishAction(a: SideBarAction){
        viewModelScope.launch {
            _action.emit(a)
        }
    }
}
