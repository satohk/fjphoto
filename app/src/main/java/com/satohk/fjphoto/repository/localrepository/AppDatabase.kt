package com.satohk.fjphoto.repository.localrepository

import androidx.room.*
import com.satohk.fjphoto.domain.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "slide_show_interval") val slideShowInterval: Int,
    @ColumnInfo(name = "slide_show_order") val slideShowOrder: Int,
    @ColumnInfo(name = "slide_show_mute") val slideShowMute: Boolean,
    @ColumnInfo(name = "slide_show_cut_play") val slideShowCutPlay: Boolean,
    @ColumnInfo(name = "num_photoGrid_columns") val numPhotoGridColumns: Int,
    @ColumnInfo(name = "screensaver_search_query") val screensaverSearchQuery: String
    )

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings where id=:id")
    fun findById(id: String): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(setting: SettingEntity)
}

@Entity(
    tableName = "photo_metadata",
    indices = [Index(value = ["account_id"])]
)
data class PhotoMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "favorite") val favorite: Boolean
)

@Dao
interface PhotoMetadataDao {
    @Query("SELECT * FROM photo_metadata where id=:id")
    fun findById(id: String): List<PhotoMetadataEntity>

    @Query("SELECT * FROM photo_metadata WHERE account_id=:accountId")
    fun findAll(accountId: String): List<PhotoMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(metadata: PhotoMetadataEntity)

    @Query("DELETE FROM photo_metadata where id=:id")
    fun delete(id: String)
}

@Entity(
    tableName = "photo_metadata_remote_cache",
    indices = [Index(value = ["account_id", "timestamp"])]
)
data class PhotoMetadataRemoteCacheEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "mime_type") val mimeType: String
)

@Dao
interface PhotoMetadataRemoteCacheDao {
    @Query("SELECT * FROM photo_metadata_remote_cache where id=:id")
    fun findById(id: String): List<PhotoMetadataRemoteCacheEntity>

    @Query("SELECT * FROM photo_metadata_remote_cache WHERE account_id=:accountId AND :startDate<=timestamp AND timestamp<=:endDate AND mime_type IN (:mimeTypes) ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    fun findAsc(accountId: String, startDate: Long, endDate: Long, mimeTypes: List<String>, limit: Int, offset: Int): List<PhotoMetadataRemoteCacheEntity>

    @Query("SELECT * FROM photo_metadata_remote_cache WHERE account_id=:accountId AND :startDate<=timestamp AND timestamp<=:endDate AND mime_type IN (:mimeTypes) ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun findDesc(accountId: String, startDate: Long, endDate: Long, mimeTypes: List<String>, limit: Int, offset: Int): List<PhotoMetadataRemoteCacheEntity>

    @Query("select timestamp from photo_metadata_remote_cache WHERE account_id=:accountId ORDER BY timestamp DESC LIMIT 1")
    fun findLastDate(accountId: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(metadata: PhotoMetadataRemoteCacheEntity)

    @Query("DELETE FROM photo_metadata_remote_cache where id=:id")
    fun delete(id: String)
}


@Database(entities = [SettingEntity::class,
    PhotoMetadataEntity::class,
    PhotoMetadataRemoteCacheEntity::class], version = 1, exportSchema=true,
    autoMigrations = [
        //AutoMigration(from = 1, to = 2)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun photoMetadataDao(): PhotoMetadataDao
    abstract fun photoMetadataRemoteCacheDao(): PhotoMetadataRemoteCacheDao
}
