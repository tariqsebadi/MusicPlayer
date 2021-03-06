package com.example.musicplayer.data.remote

import com.example.musicplayer.data.entities.Song
import com.example.musicplayer.other.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception

//implement pagination for a real app, no pagination here for simplicity
//use singleton database instance
class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    //coroutine "suspend keyword" marks the function as suspendable, asynchronous option
    //this functions gets our songs in a coroutine
    suspend fun getAllSongs(): List<Song>{
        return try {//get, gets songs, await, makes the .get() network call a suspend function and returns object type "any"
            songCollection.get().await().toObjects(Song::class.java)//turn any int object Song, otherwise catch = return empty
        }
        catch (e: Exception){
            emptyList()
        }
    }
}