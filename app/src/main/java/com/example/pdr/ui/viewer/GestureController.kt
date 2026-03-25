package com.example.pdr.ui.viewer

import android.view.MotionEvent
import android.view.View
import com.example.pdr.ui.viewer.gl.GLRenderer
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 手势控制器
 * 处理单指旋转和双指缩放
 */
class GestureController(
    private val renderer: GLRenderer
) : View.OnTouchListener {

    // 上一次触摸的点
    private var lastX = 0f
    private var lastY = 0f

    // 双指缩放相关
    private var lastSpan = 0f
    private var isZooming = false

    // 旋转灵敏度
    private var rotationSensitivity = 0.5f

    // 缩放限制
    private val minScale = 0.1f
    private val maxScale = 5f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 单指按下
                lastX = event.x
                lastY = event.y
                isZooming = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // 第二根手指按下
                if (event.pointerCount == 2) {
                    lastSpan = calculateSpan(event)
                    isZooming = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2 && isZooming) {
                    // 双指缩放
                    val currentSpan = calculateSpan(event)
                    if (lastSpan > 0) {
                        val scale = currentSpan / lastSpan
                        renderer.scale = (renderer.scale * scale).coerceIn(minScale, maxScale)
                    }
                    lastSpan = currentSpan
                } else if (event.pointerCount == 1 && !isZooming) {
                    // 单指旋转
                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    // 更新旋转角度
                    renderer.rotationY += dx * rotationSensitivity
                    renderer.rotationX += dy * rotationSensitivity

                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // 一根手指抬起
                if (event.pointerCount == 2) {
                    // 如果还剩一根手指，更新 lastX/Y 为剩余手指的位置
                    val pointerIndex = if (event.actionIndex == 0) 1 else 0
                    lastX = event.getX(pointerIndex)
                    lastY = event.getY(pointerIndex)
                }
                isZooming = false
            }

            MotionEvent.ACTION_UP -> {
                isZooming = false
            }
        }

        return true
    }

    /**
     * 计算两根手指之间的距离
     */
    private fun calculateSpan(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f

        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 重置视角
     */
    fun resetView() {
        renderer.rotationX = 0f
        renderer.rotationY = 0f
        renderer.scale = 1f
    }
}