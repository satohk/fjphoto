package com.satohk.gphotoframe.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.satohk.gphotoframe.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.time.ZonedDateTime


class PhotoGridViewModel : ViewModel() {
    private val _accountState: AccountState by inject(AccountState::class.java)

    private val _itemList = MutableStateFlow(mutableListOf<PhotoGridItem>())
    val itemList: StateFlow<List<PhotoGridItem>> get() = _itemList

    private val _readPageSize = 50

    private var _album: Album? = null
    private var _pageToken: String? = null
    private var _photoCategory: PhotoCategory? = null
    private var _startDate: ZonedDateTime? = null
    private var _endDate: ZonedDateTime? = null
    private var isLoading: Boolean = false

    private val _loadedDataSize = MutableStateFlow<Int>(0)
    val loadedDataSize: StateFlow<Int> get() = _loadedDataSize

    var lastLoadedDataSize: Int = 0
        private set

    fun loadNextImageList() {
        if(_accountState.photoRepository.value != null && !isLoading) {
            isLoading = true
            viewModelScope.launch(Dispatchers.IO) {
                val result = _accountState.photoRepository.value!!.getPhotoList(
                    _readPageSize, _pageToken, _album,
                    _photoCategory, _startDate, _endDate
                )
                val photoMetaList = result.first
                _pageToken = result.second
                _itemList.value.addAll(photoMetaList.map { PhotoGridItem(it) })
                lastLoadedDataSize = photoMetaList.size
                _loadedDataSize.value += photoMetaList.size
                isLoading = false
            }
        }
        else{
            Log.d("loadNextImageList", "_accountState.photoRepository.value is null")
        }
    }

    suspend fun loadThumbnail(photoGridItem: PhotoGridItem, width:Int?, height:Int?): Bitmap?{
        if(_accountState.photoRepository.value != null) {
            return _accountState.photoRepository.value!!.getPhotoBitmap(
                photoGridItem.photoMetaData,
                width,
                height,
                true
            )
        }
        else{
            return null
        }
    }

    data class PhotoGridItem(
        val photoMetaData: PhotoMetadata
    ) {
        companion object {
            /* DiffUtilの定義 */
            val DIFF_UTIL = object: DiffUtil.ItemCallback<PhotoGridItem>() {
                override fun areItemsTheSame(oldItem: PhotoGridItem, newItem: PhotoGridItem)
                        : Boolean {
                    return oldItem.photoMetaData.url == newItem.photoMetaData.url
                }

                override fun areContentsTheSame(oldItem: PhotoGridItem, newItem: PhotoGridItem)
                        : Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}
