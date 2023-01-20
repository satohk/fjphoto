package com.satohk.gphotoframe.repository.localrepository

import androidx.room.*

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

@Database(entities = [SettingEntity::class, PhotoMetadataEntity::class], version = 1, exportSchema=false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun photoMetadataDao(): PhotoMetadataDao
}