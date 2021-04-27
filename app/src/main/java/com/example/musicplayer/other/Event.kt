package com.example.musicplayer.other

//open, if we want to inheret, also from google convention from that version
open class Event<out T>(private val data: T) {
    var hasBeenHandles = false
    private set

    fun getContentIfNotHandled(): T? {
        return if(hasBeenHandles){
            null
        } else {
            hasBeenHandles = true
            data
        }
    }

    fun peekContent() = data
}