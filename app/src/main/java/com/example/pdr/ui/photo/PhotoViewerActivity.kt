package com.example.pdr.ui.photo

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.pdr.R
import java.io.File

/**
 * 全屏图片查看Activity，支持左右滑动切换
 */
class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tvPosition: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnDelete: ImageView
    private lateinit var adapter: PhotoViewerAdapter

    private var photoFiles: List<File> = emptyList()

    companion object {
        const val EXTRA_PHOTO_FILES = "photo_files"
        const val EXTRA_INITIAL_POSITION = "initial_position"

        /**
         * 启动Activity的辅助方法
         */
        fun start(context: Context, photos: List<File>, initialPosition: Int = 0) {
            val intent = android.content.Intent(context, PhotoViewerActivity::class.java)
            // 传递文件路径列表
            intent.putStringArrayListExtra(EXTRA_PHOTO_FILES, photos.map { it.absolutePath }.toArrayList())
            intent.putExtra(EXTRA_INITIAL_POSITION, initialPosition)
            context.startActivity(intent)
        }

        private fun List<String>.toArrayList(): java.util.ArrayList<String> {
            return java.util.ArrayList(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        viewPager = findViewById(R.id.viewPager)
        tvPosition = findViewById(R.id.tvPosition)
        btnBack = findViewById(R.id.btnBack)
        btnDelete = findViewById(R.id.btnDelete)

        // 获取传递的照片文件列表
        val photoPaths = intent.getStringArrayListExtra(EXTRA_PHOTO_FILES) ?: java.util.ArrayList()
        photoFiles = photoPaths.map { File(it) }

        val initialPosition = intent.getIntExtra(EXTRA_INITIAL_POSITION, 0)

        // 设置适配器
        adapter = PhotoViewerAdapter(photoFiles)
        viewPager.adapter = adapter

        // 设置初始位置
        if (initialPosition < photoFiles.size) {
            viewPager.setCurrentItem(initialPosition, false)
        }

        // 更新位置指示
        updatePositionIndicator()

        // 页面切换监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePositionIndicator()
            }
        })

        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }

        // 删除按钮
        btnDelete.setOnClickListener {
            showDeleteDialog()
        }
    }

    /**
     * 更新位置指示文字
     */
    private fun updatePositionIndicator() {
        val current = viewPager.currentItem + 1
        val total = photoFiles.size
        tvPosition.text = "$current / $total"
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteDialog() {
        val currentPhoto = photoFiles.getOrNull(viewPager.currentItem)
        if (currentPhoto == null) return

        AlertDialog.Builder(this)
            .setTitle("删除照片")
            .setMessage("确定要删除这张照片吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteCurrentPhoto(currentPhoto)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 删除当前照片
     */
    private fun deleteCurrentPhoto(photoFile: File) {
        if (photoFile.exists()) {
            photoFile.delete()

            // 更新列表
            photoFiles = photoFiles.filter { it != photoFile }

            if (photoFiles.isEmpty()) {
                // 没有照片了，关闭Activity
                finish()
            } else {
                // 更新适配器
                adapter.updatePhotos(photoFiles)
                updatePositionIndicator()
            }
        }
    }

    /**
     * ViewPager2 适配器
     */
    private class PhotoViewerAdapter(private var photos: List<File>) :
        RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder>() {

        fun updatePhotos(newPhotos: List<File>) {
            photos = newPhotos
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_viewer, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photoFile = photos[position]
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            holder.imageView.setImageBitmap(bitmap)
        }

        override fun getItemCount(): Int = photos.size

        class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ZoomableImageView = view.findViewById(R.id.imageViewPhoto)
        }
    }
}