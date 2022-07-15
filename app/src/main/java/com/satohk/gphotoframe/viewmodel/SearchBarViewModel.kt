package com.satohk.gphotoframe.viewmodel

import android.util.Log
import android.widget.AdapterView.INVALID_POSITION
import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.model.AccountState
import com.satohk.gphotoframe.model.MediaType
import com.satohk.gphotoframe.model.SearchQuery
import kotlinx.coroutines.flow.*
import org.koin.java.KoinJavaComponent
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class SearchBarViewModel : SideBarViewModel() {
    private val _accountState: AccountState by KoinJavaComponent.inject(AccountState::class.java)

    private val _mediaType: String? get() = index2str(mediaTypeIndex.value, mediaTypes)
    private val _contentType: String? get() = index2str(contentTypeIndex.value, contentTypes.value)
    val fromDate: ZonedDateTime?  get() = str2date(fromDateStr.value)
    val toDate: ZonedDateTime?  get() = str2date(toDateStr.value)

    val mediaTypeIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val contentTypeIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val fromDateStr: MutableStateFlow<String> = MutableStateFlow("")
    val toDateStr: MutableStateFlow<String> = MutableStateFlow("")

    private val _contentTypes: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val contentTypes: StateFlow<List<String>> get() = _contentTypes

    val mediaTypes: List<String>
        get() = MediaType.values().map{ v -> v.toString()}

    init{
        _accountState.photoRepository.onEach {
            if(it !== null){
                _contentTypes.value = it.getCategoryList()
            }
        }.launchIn(viewModelScope)

        mediaTypeIndex.onEach{ emitSideBarAction() }.launchIn(viewModelScope)
        contentTypeIndex.onEach{ emitSideBarAction() }.launchIn(viewModelScope)
        fromDateStr.onEach{ emitSideBarAction() }.launchIn(viewModelScope)
        toDateStr.onEach{ emitSideBarAction() }.launchIn(viewModelScope)
    }

    private fun index2str(index: Int, values: List<String>):String?{
        return if(index == INVALID_POSITION || index >= values.size) {
            null
        } else{
            values[index]
        }
    }

    private fun str2date(dateStr: String): ZonedDateTime?{
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneId.systemDefault())
        } catch (e: DateTimeParseException) {
            Log.d("str2date", e.toString())
            null
        }
    }

    private fun getGridContents(): GridContents {
        val from = if (fromDate !== null) fromDate else ZonedDateTime.of(
            1,
            1,
            1,
            0,
            0,
            0,
            0,
            ZoneId.systemDefault()
        )
        val to = if (toDate !== null) toDate else ZonedDateTime.of(
            9999,
            1,
            1,
            0,
            0,
            0,
            0,
            ZoneId.systemDefault()
        )
        return GridContents(
            searchQuery = SearchQuery(
                photoCategory = if (_contentType !== null) listOf(_contentType!!) else null,
                startDate = from,
                endDate = to,
                mediaType = if (_mediaType !== null) MediaType.valueOf(_mediaType!!) else MediaType.ALL
            )
        )
    }

    fun onClickMenuItem() {
        val action = SideBarAction(
            SideBarActionType.ENTER_GRID,
            gridContents = getGridContents()
        )
        _sideBarAction.value = action
    }

    private fun emitSideBarAction(){
        val action = SideBarAction(
            SideBarActionType.CHANGE_GRID,
            gridContents = getGridContents()
        )
        _sideBarAction.value = action
    }
}
