package com.example.pdr.ui.photo

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * 支持手势缩放和拖动的 ImageView
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private var mode = NONE

    // 缩放相关
    private var minScale = 1f
    private var maxScale = 4f
    private var currentScale = 1f
    private var savedScale = 1f

    // 拖动相关
    private var start = PointF()
    private var mid = PointF()

    // 边界限制
    private var viewWidth = 0
    private var viewHeight = 0
    private var imageWidth = 0
    private var imageHeight = 0

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        fitImageToView()
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null) {
            imageWidth = drawable.intrinsicWidth
            imageHeight = drawable.intrinsicHeight
            fitImageToView()
        }
    }

    /**
     * 将图片适配到视图中心
     */
    private fun fitImageToView() {
        if (viewWidth == 0 || viewHeight == 0 || imageWidth == 0 || imageHeight == 0) return

        matrix.reset()

        val scale = minOf(viewWidth.toFloat() / imageWidth, viewHeight.toFloat() / imageHeight)
        minScale = scale
        currentScale = scale

        matrix.postScale(scale, scale)

        // 将图片居中
        val dx = (viewWidth - imageWidth * scale) / 2f
        val dy = (viewHeight - imageHeight * scale) / 2f
        matrix.postTranslate(dx, dy)

        imageMatrix = matrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mode = DRAG
                start.set(event.x, event.y)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
                midPoint(mid, event)
                savedScale = currentScale
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG && currentScale > minScale) {
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                    fixTranslation()
                    start.set(event.x, event.y)
                } else if (mode == ZOOM) {
                    val scaleFactor = scaleDetector.scaleFactor
                    val newScale = savedScale * scaleFactor
                    if (newScale >= minScale && newScale <= maxScale) {
                        currentScale = newScale
                        matrix.postScale(scaleFactor, scaleFactor, mid.x, mid.y)
                        fixTranslation()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                // 如果缩放小于最小值，恢复到初始状态
                if (currentScale < minScale) {
                    currentScale = minScale
                    fitImageToView()
                }
            }
        }

        imageMatrix = matrix
        return true
    }

    /**
     * 修正边界，防止图片移出视图
     */
    private fun fixTranslation() {
        val values = FloatArray(9)
        matrix.getValues(values)

        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val scale = values[Matrix.MSCALE_X]

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        // 计算允许的移动范围
        val minX = viewWidth - scaledWidth
        val maxX = 0f
        val minY = viewHeight - scaledHeight
        val maxY = 0f

        // 修正X位置
        var newX = transX
        if (scaledWidth > viewWidth) {
            newX = Math.max(minX, Math.min(maxX, transX))
        } else {
            newX = (viewWidth - scaledWidth) / 2f
        }

        // 修正Y位置
        var newY = transY
        if (scaledHeight > viewHeight) {
            newY = Math.max(minY, Math.min(maxY, transY))
        } else {
            newY = (viewHeight - scaledHeight) / 2f
        }

        values[Matrix.MTRANS_X] = newX
        values[Matrix.MTRANS_Y] = newY
        matrix.setValues(values)
    }

    /**
     * 计算双指中点
     */
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = (event.getX(0) + event.getX(1)) / 2f
        val y = (event.getY(0) + event.getY(1)) / 2f
        point.set(x, y)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 双击切换缩放
            if (currentScale < maxScale) {
                currentScale = maxScale
                matrix.postScale(maxScale / currentScale, maxScale / currentScale, viewWidth / 2f, viewHeight / 2f)
            } else {
                currentScale = minScale
                fitImageToView()
            }
            imageMatrix = matrix
            return true
        }
    }
}