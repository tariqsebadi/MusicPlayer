package com.example.musicplayer.exoplayer

import android.widget.Button
import com.example.musicplayer.exoplayer.States.*


class FirebaseMusicSource {
    //List of lambda functions if src was initialized or not
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: States = STATE_CREATED

    set(value) {
        if (value == STATE_INITIALIZED || value == STATE_ERROR){
            synchronized(onReadyListeners){
                field = value
                onReadyListeners.forEach{ listener -> listener(state == STATE_INITIALIZED)}
            }
        }
        else{
            field = value
        }
    }

    fun whenReady(action: (Boolean) -> Unit): Boolean{
        if(state == STATE_CREATED || state == STATE_INITIALIZING){
            onReadyListeners += action
            return false
        }
        else{
            action(state == STATE_INITIALIZED)
            return true
        }
    }




}


enum class States{
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}