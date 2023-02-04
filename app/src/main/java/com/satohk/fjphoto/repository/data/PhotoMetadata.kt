package com.satohk.fjphoto.repository.data

import androidx.recyclerview.widget.DiffUtil
import com.satohk.fjphoto.domain.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class PhotoMetadataLocal(
    val id: String,
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
data class PhotoMetadataTemp(
    val aiScore: Float,
)

@Serializable
data class PhotoMetadata(
    val metadataLocal: PhotoMetadataLocal,
    val metadataRemote: PhotoMetadataRemote,
    var metadataTemp: PhotoMetadataTemp? = null
){
    fun setTemp(temp: PhotoMetadataTemp?): PhotoMetadata{
        metadataTemp = temp
        return this
    }

    companion object {
        val DIFF_UTIL = object: DiffUtil.ItemCallback<PhotoMetadata>() {
            override fun areItemsTheSame(oldItem: PhotoMetadata, newItem: PhotoMetadata)
                    : Boolean {
                return oldItem.metadataRemote.id == newItem.metadataRemote.id
            }

            override fun areContentsTheSame(oldItem: PhotoMetadata, newItem: PhotoMetadata)
                    : Boolean {
                return oldItem == newItem
            }
        }
    }
}