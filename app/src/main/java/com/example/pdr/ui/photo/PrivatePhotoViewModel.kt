package com.example.pdr.ui.photo

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream

class PrivatePhotoViewModel : ViewModel() {

    private val _photoList = MutableLiveData<List<File>>()
    val photoList: LiveData<List<File>> = _photoList

    private val _photoCount = MutableLiveData<Int>()
    val photoCount: LiveData<Int> = _photoCount

    /**
     * 获取私有照片目录
     */
    fun getPrivatePhotoDir(context: Context): File {
        val dir = File(context.filesDir, "private_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 加载私密照片列表
     */
    fun loadPhotos(context: Context) {
        val dir = getPrivatePhotoDir(context)
        val photos = dir.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        _photoList.value = photos
        _photoCount.value = photos.size
    }

    /**
     * 从图库导入图片到私密目录
     */
    fun importFromUri(context: Context, sourceUri: Uri): Boolean {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) return false

            val timestamp = System.currentTimeMillis()
            val outputFile = File(getPrivatePhotoDir(context), "photo_$timestamp.jpg")

            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            loadPhotos(context)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 保存相机拍摄的照片
     */
    fun saveCameraPhoto(context: Context, tempFile: File): Boolean {
        try {
            val timestamp = System.currentTimeMillis()
            val outputFile = File(getPrivatePhotoDir(context), "photo_$timestamp.jpg")
            tempFile.copyTo(outputFile, overwrite = true)
            loadPhotos(context)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 删除照片
     */
    fun deletePhoto(context: Context, photoFile: File): Boolean {
        try {
            if (photoFile.exists()) {
                photoFile.delete()
                loadPhotos(context)
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
}