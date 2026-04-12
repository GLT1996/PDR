package com.example.pdr.ui.photo

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R
import java.io.File

class PhotoAdapter(
    private val onItemClick: (File, Int) -> Unit,
    private val onItemLongClick: (File) -> Unit,
    private val onItemSelect: (File, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var photos: List<File> = emptyList()
    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<File>()

    fun updatePhotos(newPhotos: List<File>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(photoFile: File) {
        if (selectedItems.contains(photoFile)) {
            selectedItems.remove(photoFile)
        } else {
            selectedItems.add(photoFile)
        }
        notifyItemChanged(photos.indexOf(photoFile))
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(photos)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<File> = selectedItems.toList()

    fun getSelectedCount(): Int = selectedItems.size

    fun isSelectionMode(): Boolean = isSelectionMode

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photos[position]
        holder.bind(photoFile, position, isSelectionMode, selectedItems.contains(photoFile))
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewPhoto)
        private val selectionOverlay: FrameLayout = itemView.findViewById(R.id.selectionOverlay)

        fun bind(photoFile: File, position: Int, selectionMode: Boolean, isSelected: Boolean) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            imageView.setImageBitmap(bitmap)

            // 显示/隐藏选择覆盖层
            selectionOverlay.visibility = if (selectionMode) View.VISIBLE else View.GONE
            if (selectionMode) {
                selectionOverlay.alpha = if (isSelected) 1f else 0.3f
            }

            itemView.setOnClickListener {
                if (selectionMode) {
                    onItemSelect(photoFile, !isSelected)
                } else {
                    onItemClick(photoFile, position)
                }
            }

            itemView.setOnLongClickListener {
                if (!selectionMode) {
                    onItemLongClick(photoFile)
                }
                true
            }
        }
    }
}