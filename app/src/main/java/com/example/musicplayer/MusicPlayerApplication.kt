package com.example.musicplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


//must specify in manifest that this is our Application class
@HiltAndroidApp
class MusicPlayerApplication: Application()