package com.example.musicplayer.data.entities

//make data class inside entities package to hold songs
//Auto Parsed when we grab from firestore
data class Song (
    val mediaID:String = "",
    val title:String = "",
    val artist:String = "",
    val songURL:String = "",
    val img:String = ""
    )