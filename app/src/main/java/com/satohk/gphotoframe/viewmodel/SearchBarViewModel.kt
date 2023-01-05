package com.satohk.gphotoframe.viewmodel

import androidx.lifecycle.viewModelScope
import com.satohk.gphotoframe.domain.AccountState
import com.satohk.gphotoframe.repository.data.MediaType
import com.satohk.gphotoframe.repository.data.SearchQuery
import com.satohk.gphotoframe.repository.data.SearchQueryLocal
import com.satohk.gphotoframe.repository.data.SearchQueryRemote
import com.satohk.gphotoframe.repository.localrepository.PhotoMetadataLocalRepository
import kotlinx.coroutines.flow.*
import java.time.ZoneId
import java.time.ZonedDateTime


class SearchBarViewModel(
    private val _accountState: AccountState,
    private val _photoMetadataLocalRepo: PhotoMetadataLocalRepository
    ) : SideBarActionPublisherViewModel() {

    private val _mediaType: String? get() = Utils.spinnerIndex2str(mediaTypeIndex.value, mediaTypes)
    private val _contentType: String? get() = Utils.spinnerIndex2str(contentTypeIndex.value, contentTypes.value)
    val fromDate: ZonedDateTime?  get() = Utils.str2date(fromDateStr.value)
    val toDate: ZonedDateTime?  get() = Utils.str2date(toDateStr.value)

    val mediaTypeIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val contentTypeIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val fromDateStr: MutableStateFlow<String> = MutableStateFlow("")
    val toDateStr: MutableStateFlow<String> = MutableStateFlow("")
    val enableFilter = MutableStateFlow(false)
    val filterThreshold: MutableStateFlow<Int> = MutableStateFlow(5)

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
        enableFilter.onEach{ changeGridContents() }.launchIn(viewModelScope)
        filterThreshold.onEach { changeGridContents() }.launchIn(viewModelScope)
    }

    private suspend fun getGridContents(): GridContents {
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
        val selectedIdList = if(_accountState.activeAccount.value != null)
                                _photoMetadataLocalRepo.getAll(_accountState.activeAccount.value!!.accountId).map{it.id}
                            else listOf()
        return GridContents(
            searchQuery = SearchQuery(
                queryRemote = SearchQueryRemote(
                    photoCategory = if (_contentType !== null) listOf(_contentType!!) else null,
                    startDate = from,
                    endDate = to,
                    mediaType = if (_mediaType !== null) MediaType.valueOf(_mediaType!!) else MediaType.ALL
                ),
                queryLocal = SearchQueryLocal(
                    aiFilterEnabled=enableFilter.value,
                    aiFilterThreshold= filterThreshold.value.toFloat() / 10.0f,
                    aiFilterReferenceDataIdList=selectedIdList
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

    private suspend fun changeGridContents(){
        val action = SideBarAction(
            SideBarActionType.CHANGE_GRID,
            gridContents = getGridContents()
        )
        publishAction(action)
    }
}
