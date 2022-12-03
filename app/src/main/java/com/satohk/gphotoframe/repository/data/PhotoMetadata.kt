package com.satohk.gphotoframe.repository.data

import androidx.recyclerview.widget.DiffUtil
import com.satohk.gphotoframe.domain.ZonedDateTimeSerializer
import com.satohk.gphotoframe.viewmodel.PhotoGridViewModel
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class PhotoMetadataLocal(
    val id: String,
    val url: String,
    val favorite: Boolean = false
)

@Serializable
data class PhotoMetadataRemote(
    @Serializable(with = ZonedDateTimeSerializer::class)
    val timestamp: ZonedDateTime,
    val id: String,
    val url: String,
    val productUrl: String,
    val mimeType: String
)

@Serializable
data class PhotoMetadata(
    val metadataLocal: PhotoMetadataLocal,
    val metadataRemote: PhotoMetadataRemote
){
    companion object {
        val DIFF_UTIL = object: DiffUtil.ItemCallback<PhotoMetadata>() {
            override fun areItemsTheSame(oldItem: PhotoMetadata, newItem: PhotoMetadata)
                    : Boolean {
                return oldItem.metadataRemote.url == newItem.metadataRemote.url
            }

            override fun areContentsTheSame(oldItem: PhotoMetadata, newItem: PhotoMetadata)
                    : Boolean {
                return oldItem == newItem
            }
        }
    }
}