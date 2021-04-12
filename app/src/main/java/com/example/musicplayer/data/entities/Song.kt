package com.example.musicplayer.data.entities

//make data class inside entities package to hold songs
//Auto Parsed when we grab from firestore
data class Song (
    val artist:String = "",
    val mediaID:String = "",
    val title:String = "",
    val songURL:String = "",
    val img:String = ""
    )