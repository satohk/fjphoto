package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject


// Abstract class
open class SideBarViewModel : ViewModel() {
    protected val _sideBarAction: MutableStateFlow<SideBarAction?> = MutableStateFlow(null)
    val sideBarAction: StateFlow<SideBarAction?> get() = _sideBarAction

    fun back() {
        viewModelScope.launch {
            _sideBarAction.emit(SideBarAction(SideBarActionType.BACK))
        }
    }
}
