package com.example.musicplayer.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.musicplayer.data.entities.Song
import com.example.musicplayer.exoplayer.MusicServiceConnection
import com.example.musicplayer.exoplayer.isPlayEnabled
import com.example.musicplayer.exoplayer.isPlaying
import com.example.musicplayer.exoplayer.isPrepared
import com.example.musicplayer.other.Constants.MEDIA_ROOT_ID
import com.example.musicplayer.other.Resource
import dagger.hilt.android.scopes.ViewModelScoped

class MainViewModel @ViewModelScoped constructor(
        private val musicServieConnection: MusicServiceConnection
) : ViewModel() {
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServieConnection.isConnected
    val networkError = musicServieConnection.networkError
    val curPlayingSong = musicServieConnection.curPlayingSong
    val playbackState = musicServieConnection.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServieConnection.subscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val items = children.map {
                    Song(
                            it.mediaId!!,
                            it.description.title.toString(),
                            it.description.subtitle.toString(),
                            it.description.mediaUri.toString(),
                            it.description.iconUri.toString()
                    )
                }
                _mediaItems.postValue(Resource.success(items))
            }
        })
    }

    fun skipToNextSong(){
        musicServieConnection.transportControls.skipToNext()
    }

    fun skipToPrevSong(){
        musicServieConnection.transportControls.skipToPrevious()
    }

    fun seekTo(position: Long){
        musicServieConnection.transportControls.seekTo(position)
    }

    //default is just play song, but we can toggle by sending true
    //this is to play a song or toggle the playing state, like pausing then toggle is true
    //on new song, toggle is false
    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false){
        val isPrepared = playbackState.value?.isPrepared ?: false
        if(isPrepared && mediaItem.mediaID == curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)){
            playbackState.value?.let { playbackState ->
                when{
                    playbackState.isPlaying -> if(toggle) musicServieConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServieConnection.transportControls.play()
                    else -> Unit
                }
            }
        }
        else {
            musicServieConnection.transportControls.playFromMediaId(mediaItem.mediaID, null)
        }
    }

    override fun onCleared() {
        musicServieConnection.unSubscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback(){})
    }
}