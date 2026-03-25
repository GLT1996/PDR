package com.example.pdr.ui.viewer

import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.pdr.R
import com.example.pdr.ui.viewer.gl.GLRenderer

class ViewerFragment : Fragment() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: GLRenderer
    private lateinit var gestureController: GestureController
    private lateinit var textTitle: TextView

    // 保存当前模型文件名（用于显示标题）
    private var currentModelFileName: String? = null

    // 文件选择器
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { loadModelFromUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets(view)
        initViews(view)
        initGLSurfaceView(view)
        setupButtons(view)
    }

    private fun setupWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews(view: View) {
        textTitle = view.findViewById(R.id.textTitle)
        // 恢复标题
        currentModelFileName?.let { textTitle.text = it }
    }

    private fun initGLSurfaceView(view: View) {
        glSurfaceView = view.findViewById(R.id.glSurfaceView)

        // 创建渲染器
        renderer = GLRenderer(requireContext())

        // 先设置默认模型（在 setRenderer 之前）
        renderer.loadModel("models/cube.obj")

        // 配置 GLSurfaceView
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // 设置手势控制
        gestureController = GestureController(renderer)
        glSurfaceView.setOnTouchListener(gestureController)
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            gestureController.resetView()
        }

        view.findViewById<Button>(R.id.btnImport).setOnClickListener {
            openFilePicker()
        }
    }

    /**
     * 打开文件选择器
     */
    private fun openFilePicker() {
        // 支持所有文件类型，让用户选择 OBJ 或 STL
        openFileLauncher.launch(arrayOf("*/*"))
    }

    /**
     * 从 URI 加载模型
     */
    private fun loadModelFromUri(uri: Uri) {
        try {
            // 获取文件名
            val fileName = getFileName(uri)

            // 检查文件格式
            if (!fileName.endsWith(".obj", ignoreCase = true) &&
                !fileName.endsWith(".stl", ignoreCase = true)) {
                Toast.makeText(requireContext(), "请选择 .obj 或 .stl 文件", Toast.LENGTH_SHORT).show()
                return
            }

            // 打开输入流
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(requireContext(), "无法打开文件", Toast.LENGTH_SHORT).show()
                return
            }

            // 设置待加载模型
            renderer.loadModelFromInputStream(inputStream, fileName)

            // 在 GL 线程中执行加载
            glSurfaceView.queueEvent {
                renderer.loadPendingModelOnGLThread()
            }

            gestureController.resetView()

            // 保存文件名并更新标题
            currentModelFileName = fileName
            textTitle.text = fileName
            Toast.makeText(requireContext(), "已加载: $fileName", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * 从 URI 获取文件名
     */
    private fun getFileName(uri: Uri): String {
        var fileName = "unknown"

        // 尝试从 ContentResolver 获取
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }

        // 如果获取失败，从路径提取
        if (fileName == "unknown") {
            fileName = uri.lastPathSegment ?: "unknown"
            // 去掉路径前缀
            val lastSlash = fileName.lastIndexOf('/')
            if (lastSlash >= 0) {
                fileName = fileName.substring(lastSlash + 1)
            }
        }

        return fileName
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderer.release()
    }
}