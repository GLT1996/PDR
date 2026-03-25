package com.example.pdr.ui.viewer

import android.view.MotionEvent
import android.view.View
import com.example.pdr.ui.viewer.gl.GLRenderer
import kotlin.math.sqrt

/**
 * 手势控制器
 * 处理单指旋转、双指缩放和平移
 */
class GestureController(
    private val renderer: GLRenderer
) : View.OnTouchListener {

    // 上一次触摸的点
    private var lastX = 0f
    private var lastY = 0f

    // 双指缩放相关
    private var lastSpan = 0f
    private var lastMidX = 0f
    private var lastMidY = 0f
    private var isMultiTouch = false

    // 旋转灵敏度
    private var rotationSensitivity = 0.5f

    // 平移灵敏度
    private var panSensitivity = 0.01f

    // 缩放限制
    private val minScale = 0.1f
    private val maxScale = 5f

    // 缩放阈值（用于区分缩放和平移）
    private val zoomThreshold = 10f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 单指按下
                lastX = event.x
                lastY = event.y
                isMultiTouch = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // 第二根手指按下
                if (event.pointerCount == 2) {
                    lastSpan = calculateSpan(event)
                    lastMidX = calculateMidX(event)
                    lastMidY = calculateMidY(event)
                    isMultiTouch = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2 && isMultiTouch) {
                    // 双指操作
                    val currentSpan = calculateSpan(event)
                    val currentMidX = calculateMidX(event)
                    val currentMidY = calculateMidY(event)

                    // 计算距离变化
                    val spanDelta = kotlin.math.abs(currentSpan - lastSpan)

                    if (spanDelta > zoomThreshold) {
                        // 距离变化明显 -> 缩放
                        if (lastSpan > 0) {
                            val scale = currentSpan / lastSpan
                            renderer.scale = (renderer.scale * scale).coerceIn(minScale, maxScale)
                        }
                    } else {
                        // 距离变化不大 -> 平移
                        val dx = currentMidX - lastMidX
                        val dy = currentMidY - lastMidY
                        renderer.translateX += dx * panSensitivity
                        renderer.translateY -= dy * panSensitivity  // Y轴反向
                    }

                    lastSpan = currentSpan
                    lastMidX = currentMidX
                    lastMidY = currentMidY
                } else if (event.pointerCount == 1 && !isMultiTouch) {
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
                isMultiTouch = false
            }

            MotionEvent.ACTION_UP -> {
                isMultiTouch = false
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
     * 计算两根手指的中点 X 坐标
     */
    private fun calculateMidX(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return (event.getX(0) + event.getX(1)) / 2f
    }

    /**
     * 计算两根手指的中点 Y 坐标
     */
    private fun calculateMidY(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return (event.getY(0) + event.getY(1)) / 2f
    }

    /**
     * 重置视角
     */
    fun resetView() {
        renderer.rotationX = 0f
        renderer.rotationY = 0f
        renderer.scale = 1f
        renderer.translateX = 0f
        renderer.translateY = 0f
    }
}