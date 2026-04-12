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

    // 缩放相关
    private var minScale = 1f      // 最小缩放（适应屏幕）
    private var maxScale = 4f      // 最大缩放倍数
    private var currentScale = 1f  // 当前缩放
    private var baseScale = 1f     // 图片适应屏幕的基础缩放

    // 拖动相关
    private var lastPoint = PointF()
    private var isDragging = false

    // 边界
    private var viewWidth = 0
    private var viewHeight = 0
    private var imageWidth = 0
    private var imageHeight = 0

    // 缩放手势中心点
    private var scaleCenter = PointF()

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            scaleCenter.set(detector.focusX, detector.focusY)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // 获取缩放因子（相对于上次）
            val scaleFactor = detector.scaleFactor

            // 计算新的缩放值
            val newScale = currentScale * scaleFactor

            // 限制在范围内
            if (newScale >= minScale && newScale <= maxScale) {
                currentScale = newScale
                // 以手势中心点为基准缩放
                matrix.postScale(scaleFactor, scaleFactor, scaleCenter.x, scaleCenter.y)
                fixTranslation()
                imageMatrix = matrix
            }
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 双击切换缩放
            if (currentScale < maxScale) {
                // 放大到最大
                zoomTo(maxScale, e.x, e.y)
            } else {
                // 缩小到最小
                zoomTo(minScale, viewWidth / 2f, viewHeight / 2f)
            }
            return true
        }
    })

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

        // 计算适应屏幕的缩放比例
        baseScale = minOf(viewWidth.toFloat() / imageWidth, viewHeight.toFloat() / imageHeight)
        minScale = baseScale
        currentScale = baseScale

        matrix.postScale(baseScale, baseScale)

        // 将图片居中
        val dx = (viewWidth - imageWidth * baseScale) / 2f
        val dy = (viewHeight - imageHeight * baseScale) / 2f
        matrix.postTranslate(dx, dy)

        imageMatrix = matrix
    }

    /**
     * 缩放到指定比例
     */
    private fun zoomTo(targetScale: Float, centerX: Float, centerY: Float) {
        val scaleFactor = targetScale / currentScale
        matrix.postScale(scaleFactor, scaleFactor, centerX, centerY)
        currentScale = targetScale
        fixTranslation()
        imageMatrix = matrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 处理多点触控：阻止 ViewPager2 拦截事件，以便支持缩放
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 多点触控开始（双指按下），请求父容器不要拦截事件
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                // 多点触控结束（其中一指抬起），允许父容器拦截事件
                // 但如果还有其他手指按下，继续保持禁止拦截
                if (event.pointerCount <= 1) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 所有手指抬起或事件取消，恢复父容器拦截
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        // 先让缩放检测器处理
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // 如果正在缩放，不处理拖动
        if (scaleDetector.isInProgress) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (currentScale > minScale) {
                    isDragging = true
                    lastPoint.set(event.x, event.y)
                    // 拖动时也阻止父容器拦截
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && currentScale > minScale) {
                    val dx = event.x - lastPoint.x
                    val dy = event.y - lastPoint.y
                    matrix.postTranslate(dx, dy)
                    fixTranslation()
                    imageMatrix = matrix
                    lastPoint.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }

        return true
    }

    /**
     * 修正边界，防止图片移出视图
     */
    private fun fixTranslation() {
        if (imageWidth == 0 || imageHeight == 0) return

        val values = FloatArray(9)
        matrix.getValues(values)

        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val scaledWidth = imageWidth * currentScale
        val scaledHeight = imageHeight * currentScale

        // 计算允许的移动范围
        val minX: Float
        val maxX: Float
        val minY: Float
        val maxY: Float

        if (scaledWidth > viewWidth) {
            // 图片比视图宽，可以左右拖动
            minX = viewWidth - scaledWidth
            maxX = 0f
        } else {
            // 图片比视图窄，固定居中
            minX = (viewWidth - scaledWidth) / 2f
            maxX = minX
        }

        if (scaledHeight > viewHeight) {
            minY = viewHeight - scaledHeight
            maxY = 0f
        } else {
            minY = (viewHeight - scaledHeight) / 2f
            maxY = minY
        }

        // 修正位置
        val newX = transX.coerceIn(minX, maxX)
        val newY = transY.coerceIn(minY, maxY)

        values[Matrix.MTRANS_X] = newX
        values[Matrix.MTRANS_Y] = newY
        matrix.setValues(values)
    }
}