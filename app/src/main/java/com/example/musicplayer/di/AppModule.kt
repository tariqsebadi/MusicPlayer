package com.example.musicplayer.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.internal.managers.ApplicationComponentManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//Object does 2 things, creates a class and creates a single instance of that class (Single Instance)
//meaning, AppModule lives as long as the application does
//and provides all the dependencies

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions( // creating the glide instance .with(context)
        RequestOptions()
            .placeholder(R.drawable.ic_baseline_error_24)//change ic_IMG later
            .error(R.drawable.ic_baseline_error_24)
            .diskCacheStrategy(DiskCacheStrategy.DATA)

    )
}