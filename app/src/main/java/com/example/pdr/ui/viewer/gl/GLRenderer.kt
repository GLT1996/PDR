package com.example.pdr.ui.viewer.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.pdr.ui.viewer.parser.ObjParser
import com.example.pdr.ui.viewer.parser.StlParser
import java.io.InputStream
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 渲染器
 * 负责渲染 3D 模型
 */
class GLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var mesh: Mesh? = null

    // 模型变换参数
    var rotationX: Float = 0f
    var rotationY: Float = 0f
    var scale: Float = 1f

    // 模型颜色 (RGBA)
    var modelColor: FloatArray = floatArrayOf(0.2f, 0.6f, 0.8f, 1.0f)

    // 矩阵
    private val mvpMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // 光源位置
    private val lightPos = floatArrayOf(5f, 5f, 5f)

    // 模型边界，用于自动缩放
    private var modelMaxDimension = 1f

    // 待加载的模型文件名
    private var pendingModelFile: String? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景色
        GLES20.glClearColor(0.95f, 0.95f, 0.95f, 1.0f)

        // 启用深度测试
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 禁用背面剔除（避免剔除问题）
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // 初始化矩阵
        Matrix.setIdentityM(modelMatrix, 0)

        // 加载待加载的模型
        pendingModelFile?.let { loadModelInternal(it) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口
        GLES20.glViewport(0, 0, width, height)

        // 计算投影矩阵
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清除缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        mesh?.let { m ->
            // 计算视图矩阵 (相机位置)
            Matrix.setLookAtM(viewMatrix, 0,
                0f, 0f, 5f,  // 相机位置
                0f, 0f, 0f,  // 看向的点
                0f, 1f, 0f   // 上方向
            )

            // 计算模型矩阵
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
            Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)

            // 自动缩放模型使其适合屏幕
            val autoScale = if (modelMaxDimension > 0) 2f / modelMaxDimension else 1f
            Matrix.scaleM(modelMatrix, 0, autoScale, autoScale, autoScale)

            // 计算 MV 矩阵
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)

            // 计算 MVP 矩阵
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

            // 绘制
            m.draw(mvpMatrix, mvMatrix, lightPos, modelColor)
        }
    }

    /**
     * 加载模型文件（线程安全）
     * @param fileName assets 目录下的文件名，如 "models/cube.obj"
     */
    fun loadModel(fileName: String) {
        pendingModelFile = fileName
    }

    /**
     * 内部加载模型（在 GL 线程中调用）
     */
    private fun loadModelInternal(fileName: String) {
        try {
            val inputStream: InputStream = context.assets.open(fileName)
            val model = when {
                fileName.endsWith(".obj", ignoreCase = true) -> ObjParser.parse(inputStream)
                fileName.endsWith(".stl", ignoreCase = true) -> StlParser.parse(inputStream)
                else -> throw IllegalArgumentException("不支持的文件格式: $fileName")
            }
            inputStream.close()

            // 检查模型是否有效
            if (model.vertices.isEmpty()) {
                android.util.Log.e("GLRenderer", "Model has no vertices, creating fallback cube")
                createFallbackCube()
                return
            }

            // 计算模型边界
            val boundingBox = model.getBoundingBox()
            modelMaxDimension = boundingBox.maxDimension

            android.util.Log.d("GLRenderer", "Model loaded: ${model.getVertexCount()} vertices, maxDim=$modelMaxDimension")

            // 创建网格
            mesh?.release()
            mesh = Mesh(model)
            mesh?.initShader()

        } catch (e: Exception) {
            android.util.Log.e("GLRenderer", "Failed to load model: ${e.message}")
            e.printStackTrace()
            // 创建备用立方体
            createFallbackCube()
        }
    }

    /**
     * 创建备用立方体（程序生成）
     */
    private fun createFallbackCube() {
        val vertices = floatArrayOf(
            // 前面
            -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            // 后面
            -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f, -0.5f, -0.5f,
            // 左面
            -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f,
            // 右面
             0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,   0.5f,  0.5f,  0.5f,   0.5f, -0.5f,  0.5f,
            // 顶面
            -0.5f,  0.5f, -0.5f,  -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,
            // 底面
            -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,  -0.5f, -0.5f,  0.5f
        )

        val normals = floatArrayOf(
            // 前面
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            // 后面
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // 左面
            -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,
            -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,
            // 右面
            1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
            1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
            // 顶面
            0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
            0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
            // 底面
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f
        )

        val model = Model(vertices = vertices, normals = normals, faces = null)
        modelMaxDimension = 1f

        mesh?.release()
        mesh = Mesh(model)
        mesh?.initShader()

        android.util.Log.d("GLRenderer", "Fallback cube created")
    }

    /**
     * 释放资源
     */
    fun release() {
        mesh?.release()
        mesh = null
    }
}