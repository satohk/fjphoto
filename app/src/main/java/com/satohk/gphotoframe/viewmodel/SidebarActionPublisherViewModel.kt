package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


// Abstract class
open class SidebarActionPublisherViewModel : ViewModel() {
    private val _action = MutableSharedFlow<SidebarAction>()
    val action = _action.asSharedFlow()

    protected fun publishAction(a: SidebarAction){
        viewModelScope.launch {
            _action.emit(a)
        }
    }
}
