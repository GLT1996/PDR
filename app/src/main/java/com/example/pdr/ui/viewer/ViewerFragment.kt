package com.example.pdr.ui.viewer

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.pdr.R
import com.example.pdr.ui.viewer.gl.GLRenderer

class ViewerFragment : Fragment() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: GLRenderer
    private lateinit var gestureController: GestureController

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

    private fun initGLSurfaceView(view: View) {
        glSurfaceView = view.findViewById(R.id.glSurfaceView)

        // 创建渲染器
        renderer = GLRenderer(requireContext())

        // 配置 GLSurfaceView
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // 设置手势控制
        gestureController = GestureController(renderer)
        glSurfaceView.setOnTouchListener(gestureController)

        // 加载示例模型
        renderer.loadModel("models/cube.obj")
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            gestureController.resetView()
        }
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