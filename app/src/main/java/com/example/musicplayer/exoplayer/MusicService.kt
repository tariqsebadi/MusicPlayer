package com.example.musicplayer.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat

import androidx.media.MediaBrowserServiceCompat
import com.example.musicplayer.exoplayer.callbacks.MusicPlaybackPreparer
import com.example.musicplayer.exoplayer.callbacks.MusicPlayerEventListener
import com.example.musicplayer.exoplayer.callbacks.MusicPlayerNotificationListener
import com.example.musicplayer.other.Constants.MEDIA_ROOT_ID
import com.example.musicplayer.other.Constants.NETWORK_ERROR
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"

//this acts almost like a file manager, where albums can store albums you can browse and select more albums/songs
//hence the name, Media Browser service, service for browsing media
@AndroidEntryPoint//this is needed for every component for our dagger hilt DI
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var  firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    //services run on main thread, so we make this coroutine job plus scope it to deal with its cancelation
    //if service dies, coroutine dies = no memory leak
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)//custom servicescope = main and service merged

    //these 2 are initialized in onCreate
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForgroundSService = false

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    private var curPlayingSong: MediaMetadataCompat? = null

    companion object{
        var curSongDuration = 0L
        private set // private set does this: only change the value from within the service, but can be read outside the service
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch { firebaseMusicSource.fetchMediaData() }

        //click on notification = open our activity
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it,0)
        }
        mediaSession = MediaSessionCompat(this,SERVICE_TAG).apply {
            setSessionActivity(activityIntent)//session activity takes our intent, pending intent
            isActive = true
        }

        //mediaSession comes with session token and takes properties of our extention MediaBrowserServiceCompat()
        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){
            //update current duration of song player, but no var yet, it would be a singleton though
            //we don't have that in our metaData in our songs, we could do that but, need to update firebase
            //to save that (song duration/current song duration) as a field in firebase as miliseconds
            //we don't wanna do that, we just use exoplayer duration
            curSongDuration = exoPlayer.duration
        }

        //this lambda is called every time a new song is chosen
        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong = it
            preparePlayer(
                    firebaseMusicSource.songs,
                    it,
                    true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setPlayer(exoPlayer)//exoplayer is injected
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }

    //prepare actual exo player
    private fun preparePlayer(
            songs: List<MediaMetadataCompat>,
            itemToPlay: MediaMetadataCompat?,
            playNow: Boolean
    ){
        //play first song if no song chosen, play song 0 otherwise get index item to play
        val curSongIndex = if(curPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(curSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    //the structure we built, it has (can have) several browsable structures, like albums, recommendations etc
    //or just a song, in that case we need a root id, id that refers to the very first media items
    //in our case, its our firebase music source, just our songs
    // a more complicated app would display whatever is first
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        //our root idea is not complicated at all, we only have one list of songs
        //but we can make it very complicated with the structure we put together
        //each playlist and album can have its own root id and clients can subscribers to the root ids, that happens on onLoadChildren
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId){
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isInitizalized ->
                    if(isInitizalized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        //set playNow false so that our music doesn't start playing music when the app first launches
                        if(!isInitizalized && firebaseMusicSource.songs.isNotEmpty()){
                            preparePlayer(firebaseMusicSource.songs, firebaseMusicSource.songs[0], false)
                            isPlayerInitialized = true
                        }
                    } else {
                        mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                        //if ready but not initialized, then an error occured
                        result.sendResult(null)//wont get any songs
                    }
                }

                if (!resultsSent){
                    result.detach()
                }
            }
        }
    }

}