package com.example.pdr.ui.photo

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R
import java.io.File

class PhotoAdapter(
    private val onItemClick: (File) -> Unit,
    private val onItemLongClick: (File) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var photos: List<File> = emptyList()

    fun updatePhotos(newPhotos: List<File>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photos[position]
        holder.bind(photoFile)
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewPhoto)

        fun bind(photoFile: File) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            imageView.setImageBitmap(bitmap)

            itemView.setOnClickListener {
                onItemClick(photoFile)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(photoFile)
                true
            }
        }
    }
}