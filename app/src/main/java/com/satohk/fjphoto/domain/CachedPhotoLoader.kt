package com.satohk.fjphoto.domain

import android.accounts.NetworkErrorException
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.Math.min
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.satohk.fjphoto.repository.data.Album
import com.satohk.fjphoto.repository.data.OrderBy
import com.satohk.fjphoto.repository.data.PhotoMetadata
import com.satohk.fjphoto.repository.data.PhotoMetadataRemote
import com.satohk.fjphoto.repository.data.SearchQueryRemote
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataLocalRepository
import com.satohk.fjphoto.repository.localrepository.PhotoMetadataRemoteCacheRepository
import com.satohk.fjphoto.repository.remoterepository.PhotoRepository
import com.satohk.fjphoto.repository.remoterepository.PhotoRequestHeader
import org.koin.java.KoinJavaComponent.inject
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CachedPhotoLoader(
    private val _photoRepository: PhotoRepository,
    private val _accountId: String
) {
    private val _photoMetadataLocalRepository: PhotoMetadataLocalRepository by
                                inject(PhotoMetadataLocalRepository::class.java)

    private val _lastError = MutableStateFlow(ErrorType.ERR_NONE)
    val lastError: StateFlow<ErrorType> get() = _lastError
    private fun setError(err: ErrorType){
        Log.d("CachedPhotoRepository.setError", "this=${this}, type=${err}")
        _lastError.value = err
    }

    var photoMetadataRemoteCacheRepository: PhotoMetadataRemoteCacheRepository? = null

    private class PhotoMetadataList(
        val pageToken: String?,
        val photoMetadataList: MutableList<PhotoMetadataRemote>,
        val allLoaded: Boolean
    ){
        val size: Int get() = photoMetadataList.size
    }

    private val _albumCache = mutableListOf<Album>()
    private val _photoBitmapCache = LruCache<String, Bitmap>(1024)
    private val _albumCoverCache = LruCache<String, Bitmap>(1024)
    private val _mutex = Mutex()

    // 全listの要素数の合計をmaxSize以下とする
    private val _photoMetadataCache = object : LruCache<String, PhotoMetadataList>(10000) {
        override fun sizeOf(key:String, metadataList: PhotoMetadataList):Int{
            return min(metadataList.size, this.maxSize())
        }
    }

    private fun arg2str(vararg args: Any?): String{
        var key: String = ""
        args.forEach {
            key += it.toString() + "_"
        }
        return key
    }

    private fun isCacheUsable(searchQuery: SearchQueryRemote?): Boolean{
        return (searchQuery?.album == null) && (searchQuery?.photoCategory == null)

    }

    private suspend fun tryRequest(func: suspend () -> Unit) {
        val MAX_RETRY_COUNT = 5
        val SLEEP_MSEC = 5000L
        var err: ErrorType = ErrorType.ERR_NONE

        for (retryCount in 0..MAX_RETRY_COUNT){
            try {
                func()

                // no error
                err = ErrorType.ERR_NONE
                break
            } catch (e: NetworkErrorException) {
                err =ErrorType.ERR_COMMUNICATION
                Log.e("CachedPhotoLoader", "${e.message}")
            } catch (e: SocketTimeoutException) {
                err =ErrorType.ERR_TIMEOUT
                Log.e("CachedPhotoLoader", "${e.message}")
            } catch (e: UnknownHostException) {
                err = ErrorType.ERR_DISCONNECTED
                Log.e("CachedPhotoLoader", "${e.message}")
            }
            Log.e("CachedPhotoLoader", "Network Error $err occured. retry ct=$retryCount. sleep $SLEEP_MSEC msec")
            Thread.sleep(SLEEP_MSEC * (retryCount + 1))
        }
        if(err != ErrorType.ERR_NONE) {
            setError(err)
        }
    }

    fun photoMetadataListAllLoaded(searchQuery: SearchQueryRemote?):Boolean{
        val key = arg2str(searchQuery)
        return _photoMetadataCache.get(key).allLoaded
    }

    suspend fun getPhotoMetadataList(offset:Int, size:Int, searchQuery: SearchQueryRemote?):List<PhotoMetadata>{
        val key = arg2str(searchQuery)
        var list = _photoMetadataCache.get(key)

        if((list == null) || (!list.allLoaded && list.size < offset + size)){
            tryRequest {
                val bulkLoadSize = 60
                var addMetadataList: List<PhotoMetadataRemote>? = null
                var nextPageToken: String? = null
                //if(isCacheUsable(searchQuery) && photoMetadataRemoteCacheRepository != null){
                if(false){  // disable remotecache
                    Log.d("getPhotoMetadataList", "use disk cache")
                    addMetadataList = photoMetadataRemoteCacheRepository?.get(
                        _accountId,
                        searchQuery?.startDate,
                        searchQuery?.endDate,
                        searchQuery?.mediaType,
                        bulkLoadSize,
                        offset,
                        searchQuery?.orderBy == OrderBy.CREATION_TIME_DESC
                        )
                    nextPageToken = if(addMetadataList!!.size < bulkLoadSize) null else "dummy"
                }
                else {
                    Log.d("getPhotoMetadataList", "use memory cache")
                    _mutex.withLock {
                        val pageToken = list?.pageToken
                        val res = _photoRepository.getNextPhotoMetadataList(
                            bulkLoadSize, pageToken, searchQuery
                        )
                        addMetadataList = res.first
                        nextPageToken = res.second
                    }
                }
                val resultList = list?.photoMetadataList ?: mutableListOf()
                if(addMetadataList != null) {
                    resultList.addAll(addMetadataList!!)
                }
                list = PhotoMetadataList(nextPageToken, resultList, nextPageToken == null)
                _photoMetadataCache.put(key, list)
            }
        }

        return if((list != null) && (list.size >= offset)){
            list.photoMetadataList.subList(offset, min(offset + size, list.size)).map{
                it ->
                PhotoMetadata(
                    _photoMetadataLocalRepository.get(it.id),
                    it
                )
            }
        } else{
            listOf()
        }
    }

    suspend fun getAlbumList():List<Album> {
        if(_albumCache.size == 0) {
            tryRequest {
                _albumCache.addAll(_photoRepository.getAlbumList())
            }
        }
        return _albumCache
    }

    suspend fun getPhotoMetadata(photoId: String):PhotoMetadataRemote{
        return _photoRepository.getPhotoMetadata(photoId)
    }

    suspend fun getPhotoBitmap(photo: PhotoMetadataRemote, width:Int?, height:Int?, cropFlag:Boolean?):Bitmap? {
        val key = arg2str(photo, width, height, cropFlag)
        var res = _photoBitmapCache.get(key)
        if(res == null){
            tryRequest {
                //val photo2 = getPhotoMetadata(photo.id)  // urlの有効期限が切れていることがあるので、再度メタデータを取得 -> cacheを無効化したので、再取得は必要なくなった
                res = _photoRepository.getPhotoBitmap(photo, width, height, cropFlag)
                if(res != null) {
                    _photoBitmapCache.put(key, res)
                }
            }
        }
        return res
    }

    suspend fun getAlbumCoverPhoto(album: Album, width:Int?, height:Int?, cropFlag:Boolean?): Bitmap? {
        val key = arg2str(album, width, height, cropFlag)
        var res = _albumCoverCache.get(key)
        if(res == null){
            tryRequest {
                res = _photoRepository.getAlbumCoverPhoto(album, width, height, cropFlag)
                _albumCoverCache.put(key, res)
            }
        }
        return res
    }

    suspend fun getMediaAccessHeaderAndUrl(media: PhotoMetadataRemote): Pair<PhotoRequestHeader, String> {
        //val media2 = getPhotoMetadata(media.id)  // urlの有効期限が切れていることがあるので、再度メタデータを取得  ->   cacheを無効化したので、再取得は必要なくなった
        return _photoRepository.getMediaAccessHeaderAndUrl(media)
    }

    fun getCategoryList(): List<String>{
        return _photoRepository.getCategoryList()
    }

    enum class ErrorType{
        ERR_NONE,
        ERR_COMMUNICATION,
        ERR_TIMEOUT,
        ERR_DISCONNECTED,
    }
}