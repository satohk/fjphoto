package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.repository.entity.MediaType
import com.satohk.gphotoframe.repository.entity.SearchQuery
import com.satohk.gphotoframe.repository.entity.SearchQueryRemote
import kotlinx.coroutines.flow.*
import java.time.ZoneId
import java.time.ZonedDateTime


class SearchBarViewModel(
    _accountState: AccountState
    ) : SideBarActionPublisherViewModel() {

    private val _mediaType: String? get() = Utils.spinnerIndex2str(mediaTypeIndex.value, mediaTypes)
    private val _contentType: String? get() = Utils.spinnerIndex2str(contentTypeIndex.value, contentTypes.value)
    val fromDate: ZonedDateTime?  get() = Utils.str2date(fromDateStr.value)
    val toDate: ZonedDateTime?  get() = Utils.str2date(toDateStr.value)

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

        mediaTypeIndex.onEach{ changeGridContents() }.launchIn(viewModelScope)
        contentTypeIndex.onEach{ changeGridContents() }.launchIn(viewModelScope)
        fromDateStr.onEach{ changeGridContents() }.launchIn(viewModelScope)
        toDateStr.onEach{ changeGridContents() }.launchIn(viewModelScope)
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
                queryRemote = SearchQueryRemote(
                    photoCategory = if (_contentType !== null) listOf(_contentType!!) else null,
                    startDate = from,
                    endDate = to,
                    mediaType = if (_mediaType !== null) MediaType.valueOf(_mediaType!!) else MediaType.ALL
                )
            )
        )
    }

    fun enterToGrid() {
        val action = SideBarAction(
            SideBarActionType.ENTER_GRID,
            gridContents = null
        )
        publishAction(action)
    }

    fun goBack(){
        val action = SideBarAction(
            SideBarActionType.BACK,
            gridContents = null
        )
        publishAction(action)
    }

    private fun changeGridContents(){
        val action = SideBarAction(
            SideBarActionType.CHANGE_GRID,
            gridContents = getGridContents()
        )
        publishAction(action)
    }
}
