package com.satohk.gphotoframe.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.time.ZonedDateTime


class PhotoGridViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)

    private val _urlList = MutableStateFlow(mutableListOf<PhotoGridItem>())
    val urlList: StateFlow<List<PhotoGridItem>> get() = _urlList

    private val _readPageSize = 50

    private var _showPoint = 0
    private var _album: Album? = null
    private var _pageToken: String? = null
    private var _photoCategory: PhotoCategory? = null
    private var _startDate: ZonedDateTime? = null
    private var _endDate: ZonedDateTime? = null

    private val _loadedDataSize = MutableStateFlow<Int>(0)
    val loadedDataSize: StateFlow<Int> get() = _loadedDataSize

    init{
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (_showPoint + _readPageSize >= _urlList.value.size && _accountState.photoRepository.value != null) {
                    val photoMetaList = _accountState.photoRepository.value!!.getPhotoList(
                        _readPageSize, _pageToken, _album,
                        _photoCategory, _startDate, _endDate
                    )
                    _urlList.value.addAll(photoMetaList.map { PhotoGridItem(Uri.parse(it.url)) })
                    _loadedDataSize.value += photoMetaList.size
                }
                delay(10000)
            }
        }
    }

    fun getPhotoItem(position: Int): PhotoGridItem{
        return _urlList.value[position]
    }

    fun setShowPoint(showPoint: Int){
        _showPoint = showPoint
    }

    data class PhotoGridItem(
        val uri: Uri /* 現状はURIのみ保持する */
    ) {
        companion object {
            /* DiffUtilの定義 */
            val DIFF_UTIL = object: DiffUtil.ItemCallback<PhotoGridItem>() {
                override fun areItemsTheSame(oldItem: PhotoGridItem, newItem: PhotoGridItem)
                        : Boolean {
                    return oldItem.uri == newItem.uri
                }

                override fun areContentsTheSame(oldItem: PhotoGridItem, newItem: PhotoGridItem)
                        : Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}
