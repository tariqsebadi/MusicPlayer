package com.example.musicplayer.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.example.musicplayer.data.remote.MusicDatabase
import com.example.musicplayer.exoplayer.State.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

//gets all the songs from firebase and converts into a format we need for our service, media session
class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
) {

    //hold meta info about the song, public to service acccess
    var songs = emptyList<MediaMetadataCompat>()

    //this just holds metadata of our songs
    suspend fun fetchMediaData() = withContext(Dispatchers.IO){
        state = STATE_INITIALIZING
        val allSongs = musicDatabase.getAllSongs()
        //our song.kt type is different from songs type (MediaMetaDataCompat) so we need to map them together
        songs = allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.artist)//putString is very similar to shared preferences
                .putString(METADATA_KEY_MEDIA_ID, song.mediaID)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.img)
                .putString(METADATA_KEY_MEDIA_URI, song.songURL)
                .putString(METADATA_KEY_ART_URI, song.img)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.artist)
                .build()
        }
        state = STATE_INITIALIZED//triggers our setter
    }

    //playlist - one song finished, the next one plays
    //this is just from where exoplayer can stream that song
    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory):ConcatenatingMediaSource {//from serviceModule
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->//creating single media sources in a list of media sources
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    //media items is a single item in our list, browsable item, the item can be an album holding songs, or just a song, or album holding albums
    fun asMediaItems() = songs.map { song ->//but ours just maps to songs
        val description = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(description,FLAG_PLAYABLE)
    }

    //when we download our music from firebase, its in the bg thread, we need a way to know if its finished downloading
    //this list of lambda functions that tells us if its initialized or not
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = STATE_CREATED
        set(value) {//changing value of multiple threads at once, threadsafe
            if (value == STATE_INITIALIZED || value == STATE_ERROR){
                synchronized(onReadyListeners){//synchronized, what happens inside this block only happens on the same thread
                    //aka, no other thread can access onReadyListeners at the same time
                    field = value //field is current value and value is the new value, aka state
                    onReadyListeners.forEach{listener ->//we call the lambda function, the music source is done,
                        //our state is initialized, so lambda is true, otherwise our state is state error therefore false
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else{
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit):Boolean{
        return if(state == STATE_CREATED || state == STATE_INITIALIZING){
            onReadyListeners += action
            false
        } else{
            action(state == STATE_INITIALIZED)
            true
        }
    }
}

//defines several states our music source can be in
//google sample project, universal android music player
enum class State{
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}
/*
I will later have a list of songs from firebase
we need a state variable to track the state of the music source as it takes time to get/dl the music files
before we download, we set the state to initializing
after its downloaded, we set state to initialized
otherwise if its errored, set error
I can then use the states to schedule an action when the music is finished loading
whenReady, this action executes a piece of code only when the music source is finished loading
*/