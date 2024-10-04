package com.satohk.fjphoto.view

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
class VideoCache(context: Context) {
    lateinit var  cache: SimpleCache

    init {
        val maxBytes: Long = 1024 * 1024 * 100
        val databaseProvider = StandaloneDatabaseProvider(context)
        cache = SimpleCache(
            File(context.filesDir, "media-cache"),
            LeastRecentlyUsedCacheEvictor(maxBytes),
            databaseProvider
        )
        Log.d("mediaCache keys", cache.getKeys().toString())
    }
}