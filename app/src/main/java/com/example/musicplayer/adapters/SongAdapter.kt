package com.example.musicplayer.adapters

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.musicplayer.data.entities.Song
import javax.inject.Inject

class SongAdapter @Inject constructor(
        private val glid: RequestManager
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val diffCallback = object : DiffUtil.ItemCallback<Song>(){
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.mediaID == newItem.mediaID
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }
}