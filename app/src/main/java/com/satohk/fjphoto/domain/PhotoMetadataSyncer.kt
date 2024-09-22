package com.satohk.fjphoto.domain

import android.accounts.NetworkErrorException
import android.util.Log
import com.satohk.fjphoto.repository.data.OrderBy
import com.satohk.fjphoto.repository.data.PhotoMetadataRemote
import com.satohk.fjphoto.repository.data.SearchQueryRemote
import com.satohk.fjphoto.repository.localrepository.AppDatabase
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataRemoteCacheEntity
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataRemoteCacheRepository
import com.satohk.fjphoto.repository.remoterepository.PhotoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.ZonedDateTime

class PhotoMetadataSyncer(private val _photoRepository: PhotoRepository,
                          private val _photoMetadataRemoteCacheRepository: PhotoMetadataRemoteCacheRepository,
                          private val _accountId: String) {

    private val _synced = MutableStateFlow<Boolean>(false)
    val synced: StateFlow<Boolean> get() = _synced

    suspend fun syncAll(){
        val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        withContext(ioDispatcher) {
            var pageToken: String? = null
            val bulkLoadSize = 100
            val lastData = _photoMetadataRemoteCacheRepository.getLast(_accountId)
            val searchQuery = if(lastData != null){
                SearchQueryRemote(startDate = lastData.timestamp, orderBy= OrderBy.CREATION_TIME_ASC)
            }else{
                SearchQueryRemote(orderBy= OrderBy.CREATION_TIME_ASC)
            }
            Log.d("syncAll start", "$searchQuery")
            while (true) {
                try {
                    val res = _photoRepository.getNextPhotoMetadataList(
                        bulkLoadSize, pageToken, searchQuery
                    )
                    val resultList: List<PhotoMetadataRemote> = res.first

                    for (metaData in resultList) {
                        _photoMetadataRemoteCacheRepository.add(_accountId, metaData)
                    }
                    pageToken = res.second
                    if (pageToken == null) {
                        // all loaded
                        break
                    }
                }
                catch(e: NetworkErrorException){
                    Log.d("syncAll", "error $e")
                    Thread.sleep(5000)
                }
                catch(e: SocketTimeoutException){
                    Log.d("syncAll", "error $e")
                    Thread.sleep(5000)
                }
                catch(e: UnknownHostException){
                    Log.d("syncAll", "error $e")
                    Thread.sleep(5000)
                }
            }
        }

        _synced.value = true
    }
}