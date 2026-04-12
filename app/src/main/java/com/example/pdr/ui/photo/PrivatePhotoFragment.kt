package com.example.pdr.ui.photo

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdr.R
import com.google.android.material.button.MaterialButton
import java.io.File

class PrivatePhotoFragment : Fragment() {

    private val viewModel: PrivatePhotoViewModel by viewModels()
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnViewPhotos: MaterialButton
    private lateinit var tvPhotoCount: android.widget.TextView

    // 相机临时文件
    private var cameraTempFile: File? = null

    // 权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    // 相机拍摄回调
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempFile != null) {
            viewModel.saveCameraPhoto(requireContext(), cameraTempFile!!)
            Toast.makeText(requireContext(), "照片已保存", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "拍照失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 图库选择回调
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val success = viewModel.importFromUri(requireContext(), it)
            if (success) {
                Toast.makeText(requireContext(), "照片已导入", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "导入失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_private_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        btnViewPhotos = view.findViewById(R.id.btnViewPhotos)
        tvPhotoCount = view.findViewById(R.id.tvPhotoCount)

        // 加载照片数量
        viewModel.loadPhotos(requireContext())
        viewModel.photoCount.observe(viewLifecycleOwner) { count ->
            tvPhotoCount.text = "已保存 $count 张私密照片"
        }

        // 生成照片按钮 - 弹出选择对话框
        btnAddPhoto.setOnClickListener {
            showAddPhotoDialog()
        }

        // 查看图片按钮
        btnViewPhotos.setOnClickListener {
            findNavController().navigate(R.id.action_private_to_list)
        }
    }

    /**
     * 显示添加照片选择对话框
     */
    private fun showAddPhotoDialog() {
        val options = arrayOf("相机拍摄", "从图库选择")
        AlertDialog.Builder(requireContext())
            .setTitle("选择图片来源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> checkStoragePermissionAndOpen()
                }
            }
            .show()
    }

    /**
     * 检查相机权限并打开相机
     */
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * 检查存储权限并打开图库
     */
    private fun checkStoragePermissionAndOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        // 创建临时文件
        cameraTempFile = File(requireContext().filesDir, "temp_camera_${System.currentTimeMillis()}.jpg")

        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.example.pdr.fileprovider",
            cameraTempFile!!
        )

        cameraLauncher.launch(photoUri)
    }

    /**
     * 打开图库
     */
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
}