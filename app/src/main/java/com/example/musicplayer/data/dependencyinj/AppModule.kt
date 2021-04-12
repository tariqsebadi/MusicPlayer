package com.example.musicplayer.data.dependencyinj

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.musicplayer.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton // just making sure its really a single instance.. redundant though
    @Provides
    fun glideInstanceProvider(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .apply(){
            placeholderDrawable
            error(R.drawable.ic_image)
            diskCacheStrategy(DiskCacheStrategy.DATA)
        }
//            RequestOptions()
//            .placeholder(R.drawable.ic_image)
//            .error(R.drawable.ic_image)
//            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )


}