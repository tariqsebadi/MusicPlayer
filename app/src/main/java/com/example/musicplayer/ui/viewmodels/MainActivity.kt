package com.example.musicplayer.ui.viewmodels

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.RequestManager
import com.example.musicplayer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}