package com.example.pdr.ui.photo

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R

class PhotoListFragment : Fragment() {

    private val viewModel: PrivatePhotoViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: PhotoAdapter

    // 批量选择模式相关
    private lateinit var headerLayout: LinearLayout
    private lateinit var batchToolbar: LinearLayout
    private lateinit var btnBatchDelete: ImageView
    private lateinit var btnCancelBatch: ImageView
    private lateinit var btnSelectAll: ImageView
    private lateinit var btnConfirmDelete: ImageView
    private lateinit var tvSelectedCount: TextView

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
        headerLayout = view.findViewById(R.id.headerLayout)
        batchToolbar = view.findViewById(R.id.batchToolbar)
        btnBatchDelete = view.findViewById(R.id.btnBatchDelete)
        btnCancelBatch = view.findViewById(R.id.btnCancelBatch)
        btnSelectAll = view.findViewById(R.id.btnSelectAll)
        btnConfirmDelete = view.findViewById(R.id.btnConfirmDelete)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)

        // 设置状态栏padding，避免刘海屏遮挡
        view.setOnApplyWindowInsetsListener { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            headerLayout.setPadding(0, statusBarHeight, 0, 0)
            batchToolbar.setPadding(0, statusBarHeight, 8, 8)
            insets
        }

        // 设置网格布局（2列）
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // 创建适配器
        adapter = PhotoAdapter(
            onItemClick = { photoFile, position -> showFullImage(position) },
            onItemLongClick = { photoFile -> showDeleteDialog(photoFile) },
            onItemSelect = { photoFile, _ -> handleItemSelect(photoFile) }
        )
        recyclerView.adapter = adapter

        // 批量删除按钮
        btnBatchDelete.setOnClickListener {
            enterSelectionMode()
        }

        // 取消批量选择
        btnCancelBatch.setOnClickListener {
            exitSelectionMode()
        }

        // 全选
        btnSelectAll.setOnClickListener {
            adapter.selectAll()
            updateSelectedCount()
        }

        // 确认删除选中项
        btnConfirmDelete.setOnClickListener {
            showBatchDeleteDialog()
        }

        // 加载照片
        viewModel.loadPhotos(requireContext())

        // 观察照片列表变化
        viewModel.photoList.observe(viewLifecycleOwner) { photos ->
            adapter.updatePhotos(photos)
            emptyState.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
            btnBatchDelete.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE

            // 如果照片被全部删除，退出选择模式
            if (photos.isEmpty() && adapter.isSelectionMode()) {
                exitSelectionMode()
            }
        }
    }

    /**
     * 进入批量选择模式
     */
    private fun enterSelectionMode() {
        adapter.setSelectionMode(true)
        headerLayout.visibility = View.GONE
        batchToolbar.visibility = View.VISIBLE
        updateSelectedCount()
    }

    /**
     * 退出批量选择模式
     */
    private fun exitSelectionMode() {
        adapter.setSelectionMode(false)
        headerLayout.visibility = View.VISIBLE
        batchToolbar.visibility = View.GONE
    }

    /**
     * 处理单项选择
     */
    private fun handleItemSelect(photoFile: java.io.File) {
        adapter.toggleSelection(photoFile)
        updateSelectedCount()
    }

    /**
     * 更新选中数量显示
     */
    private fun updateSelectedCount() {
        val count = adapter.getSelectedCount()
        tvSelectedCount.text = "已选择 $count 张"
    }

    /**
     * 显示全屏图片（支持左右滑动切换）
     */
    private fun showFullImage(position: Int) {
        val photos = viewModel.photoList.value ?: emptyList()
        if (photos.isEmpty()) return

        PhotoViewerActivity.start(requireContext(), photos, position)
    }

    /**
     * 显示单张删除确认对话框
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

    /**
     * 显示批量删除确认对话框
     */
    private fun showBatchDeleteDialog() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("提示")
                .setMessage("请先选择要删除的照片")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("批量删除")
            .setMessage("确定要删除已选择的 ${selectedItems.size} 张照片吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deletePhotos(requireContext(), selectedItems)
                exitSelectionMode()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}