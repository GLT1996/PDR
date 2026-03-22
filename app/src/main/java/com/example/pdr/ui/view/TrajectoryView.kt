package com.example.pdr.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.pdr.data.model.TrajectoryPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 轨迹绘制视图
 * 自定义View用于绘制PDR轨迹路线
 */
class TrajectoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 轨迹数据
    private val trajectoryPoints = mutableListOf<TrajectoryPoint>()

    // 绘制参数
    private val trajectoryPaint = Paint().apply {
        color = Color.parseColor("#2196F3")  // 蓝色
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")  // 绿色
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val startPointPaint = Paint().apply {
        color = Color.parseColor("#F44336")  // 红色
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val endPointPaint = Paint().apply {
        color = Color.parseColor("#FF9800")  // 橙色
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 28f
        isAntiAlias = true
    }

    // 变换参数
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var minScale = 0.1f
    private var maxScale = 10f

    // 路径
    private val trajectoryPath = Path()

    // 手势检测
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    // 自动居中
    private var autoCenter = true

    // 坐标范围
    private var minX = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var minY = Float.MAX_VALUE
    private var maxY = Float.MIN_VALUE

    // 网格大小（米）
    private val gridSize = 5f

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            scale = (scale * scaleFactor).coerceIn(minScale, maxScale)
            autoCenter = false
            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            offsetX -= distanceX
            offsetY -= distanceY
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 双击重置视图
            resetView()
            return true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (trajectoryPoints.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        // 保存画布状态
        canvas.save()

        // 应用变换
        val centerX = width / 2f
        val centerY = height / 2f

        if (autoCenter) {
            // 自动居中模式
            val dataCenterX = (minX + maxX) / 2
            val dataCenterY = (minY + maxY) / 2

            // 计算自适应缩放
            val dataWidth = maxX - minX
            val dataHeight = maxY - minY
            val viewWidth = width - 100f
            val viewHeight = height - 100f

            if (dataWidth > 0 && dataHeight > 0) {
                scale = min(viewWidth / dataWidth, viewHeight / dataHeight)
                scale = max(scale, minScale)
            }

            offsetX = centerX - dataCenterX * scale
            offsetY = centerY - dataCenterY * scale
        }

        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        // 绘制网格
        drawGrid(canvas)

        // 绘制轨迹
        drawTrajectory(canvas)

        // 绘制点
        drawPoints(canvas)

        // 恢复画布状态
        canvas.restore()

        // 绘制信息
        drawInfo(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = "开始记录以显示轨迹"
        val textWidth = textPaint.measureText(text)
        canvas.drawText(text, (width - textWidth) / 2, height / 2f, textPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        // 计算可见范围
        val left = (0 - offsetX) / scale
        val right = (width - offsetX) / scale
        val top = (0 - offsetY) / scale
        val bottom = (height - offsetY) / scale

        // 绘制垂直线
        val startX = (left / gridSize).toInt() * gridSize
        var x = startX.toFloat()
        while (x <= right) {
            canvas.drawLine(x, top, x, bottom, gridPaint)
            x += gridSize
        }

        // 绘制水平线
        val startY = (top / gridSize).toInt() * gridSize
        var y = startY.toFloat()
        while (y <= bottom) {
            canvas.drawLine(left, y, right, y, gridPaint)
            y += gridSize
        }
    }

    private fun drawTrajectory(canvas: Canvas) {
        if (trajectoryPoints.size < 2) return

        trajectoryPath.reset()
        trajectoryPath.moveTo(trajectoryPoints[0].x, trajectoryPoints[0].y)

        for (i in 1 until trajectoryPoints.size) {
            trajectoryPath.lineTo(trajectoryPoints[i].x, trajectoryPoints[i].y)
        }

        canvas.drawPath(trajectoryPath, trajectoryPaint)
    }

    private fun drawPoints(canvas: Canvas) {
        if (trajectoryPoints.isEmpty()) return

        val pointRadius = 8f

        // 绘制起点
        val startPoint = trajectoryPoints.first()
        canvas.drawCircle(startPoint.x, startPoint.y, pointRadius * 1.5f, startPointPaint)

        // 绘制终点（当前位置）
        val endPoint = trajectoryPoints.last()
        canvas.drawCircle(endPoint.x, endPoint.y, pointRadius * 1.5f, endPointPaint)

        // 绘制方向指示
        if (trajectoryPoints.size > 1) {
            val heading = endPoint.heading
            val arrowLength = 15f
            val arrowX = endPoint.x + arrowLength * kotlin.math.sin(heading)
            val arrowY = endPoint.y + arrowLength * kotlin.math.cos(heading)

            val arrowPaint = Paint().apply {
                color = Color.parseColor("#FF9800")
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(endPoint.x, endPoint.y, arrowX, arrowY, arrowPaint)
        }
    }

    private fun drawInfo(canvas: Canvas) {
        if (trajectoryPoints.isEmpty()) return

        val info = buildString {
            append("步数: ${trajectoryPoints.last().stepCount}")
            append("  距离: ${String.format("%.1f", calculateTotalDistance())}m")
        }

        canvas.drawText(info, 20f, 40f, textPaint)

        // 绘制缩放比例
        val scaleInfo = "缩放: ${String.format("%.1f", scale)}x"
        canvas.drawText(scaleInfo, 20f, 70f, textPaint)
    }

    private fun calculateTotalDistance(): Float {
        var distance = 0f
        for (i in 1 until trajectoryPoints.size) {
            val dx = trajectoryPoints[i].x - trajectoryPoints[i - 1].x
            val dy = trajectoryPoints[i].y - trajectoryPoints[i - 1].y
            distance += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return distance
    }

    /**
     * 添加轨迹点
     */
    fun addPoint(point: TrajectoryPoint) {
        trajectoryPoints.add(point)
        updateBounds(point)
        invalidate()
    }

    /**
     * 设置轨迹点列表
     */
    fun setPoints(points: List<TrajectoryPoint>) {
        trajectoryPoints.clear()
        trajectoryPoints.addAll(points)
        updateAllBounds()
        invalidate()
    }

    /**
     * 清除轨迹
     */
    fun clear() {
        trajectoryPoints.clear()
        resetBounds()
        invalidate()
    }

    private fun updateBounds(point: TrajectoryPoint) {
        minX = min(minX, point.x)
        maxX = max(maxX, point.x)
        minY = min(minY, point.y)
        maxY = max(maxY, point.y)
    }

    private fun updateAllBounds() {
        resetBounds()
        trajectoryPoints.forEach { updateBounds(it) }
    }

    private fun resetBounds() {
        minX = Float.MAX_VALUE
        maxX = Float.MIN_VALUE
        minY = Float.MAX_VALUE
        maxY = Float.MIN_VALUE
    }

    /**
     * 重置视图
     */
    fun resetView() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        autoCenter = true
        invalidate()
    }

    /**
     * 获取轨迹点数量
     */
    fun getPointCount(): Int = trajectoryPoints.size
}