package com.example.pdr.ui.photo

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R

class PhotoListFragment : Fragment() {

    private val viewModel: PrivatePhotoViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: PhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_photo_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewPhotos)
        emptyState = view.findViewById(R.id.emptyState)

        // 设置网格布局（2列）
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // 创建适配器
        adapter = PhotoAdapter(
            onItemClick = { photoFile -> showFullImage(photoFile) },
            onItemLongClick = { photoFile -> showDeleteDialog(photoFile) }
        )
        recyclerView.adapter = adapter

        // 加载照片
        viewModel.loadPhotos(requireContext())

        // 观察照片列表变化
        viewModel.photoList.observe(viewLifecycleOwner) { photos ->
            adapter.updatePhotos(photos)
            emptyState.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    /**
     * 显示全屏图片
     */
    private fun showFullImage(photoFile: java.io.File) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_full_image, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageViewFull)

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        imageView.setImageBitmap(bitmap)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteDialog(photoFile: java.io.File) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除照片")
            .setMessage("确定要删除这张照片吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deletePhoto(requireContext(), photoFile)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}