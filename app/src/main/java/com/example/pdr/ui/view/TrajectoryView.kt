package com.example.pdr.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.pdr.data.model.TrajectoryPoint
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

    // 绘制参数 - 使用屏幕像素单位
    private val trajectoryPaint = Paint().apply {
        color = Color.parseColor("#2196F3")  // 蓝色
        strokeWidth = 8f  // 像素
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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
        textSize = 32f
        isAntiAlias = true
    }

    // 变换参数
    private var scale = 50f  // 像素/米，初始缩放：1米 = 50像素
    private var offsetX = 0f
    private var offsetY = 0f
    private val minScale = 5f    // 最小缩放：1米 = 5像素
    private val maxScale = 200f  // 最大缩放：1米 = 200像素

    // 手势检测
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    // 自动居中
    private var autoCenter = true

    // 坐标范围（米）
    private var minX = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var minY = Float.MAX_VALUE
    private var maxY = Float.MIN_VALUE

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
            autoCenter = false
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
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

            // 只有当数据范围足够大时才自动缩放
            if (dataWidth > 0.1f && dataHeight > 0.1f) {
                val autoScale = min(viewWidth / dataWidth, viewHeight / dataHeight)
                scale = autoScale.coerceIn(minScale, maxScale)
            }

            offsetX = centerX - dataCenterX * scale
            // Y轴翻转：数据Y向上，屏幕Y向下
            offsetY = centerY + dataCenterY * scale
        }

        // 应用变换：先平移，再缩放
        // Y轴翻转：scale(1, -1) 使Y轴反向
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, -scale)  // Y轴翻转

        // 绘制网格（米为单位）
        drawGrid(canvas)

        // 绘制轨迹
        drawTrajectory(canvas)

        // 恢复画布状态（点在屏幕空间绘制）
        canvas.restore()

        // 绘制起点和终点（屏幕像素单位）
        drawPointsInScreenSpace(canvas)

        // 绘制信息
        drawInfo(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = "开始记录以显示轨迹"
        val textWidth = textPaint.measureText(text)
        canvas.drawText(text, (width - textWidth) / 2, height / 2f, textPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        // 网格大小：5米
        val gridSpacing = 5f

        // 计算可见范围（米）
        // 注意：canvas Y轴已翻转，所以top和bottom要交换
        val left = -offsetX / scale
        val right = (width - offsetX) / scale
        val bottom = -offsetY / scale   // 屏幕上方对应数据下方
        val top = (height - offsetY) / scale  // 屏幕下方对应数据上方

        // 保存当前线宽
        val originalStrokeWidth = gridPaint.strokeWidth
        // 设置线宽为屏幕像素（除以scale转回数据空间）
        gridPaint.strokeWidth = 1f / scale

        // 绘制垂直线
        var x = (left / gridSpacing).toInt() * gridSpacing.toFloat()
        while (x <= right) {
            canvas.drawLine(x, top, x, bottom, gridPaint)
            x += gridSpacing
        }

        // 绘制水平线
        var y = (bottom / gridSpacing).toInt() * gridSpacing.toFloat()
        while (y <= top) {
            canvas.drawLine(left, y, right, y, gridPaint)
            y += gridSpacing
        }

        // 恢复线宽
        gridPaint.strokeWidth = originalStrokeWidth
    }

    private fun drawTrajectory(canvas: Canvas) {
        if (trajectoryPoints.size < 2) return

        // 设置线宽为屏幕像素对应的米数
        trajectoryPaint.strokeWidth = 8f / scale

        val path = Path()
        path.moveTo(trajectoryPoints[0].x, trajectoryPoints[0].y)

        for (i in 1 until trajectoryPoints.size) {
            path.lineTo(trajectoryPoints[i].x, trajectoryPoints[i].y)
        }

        canvas.drawPath(path, trajectoryPaint)
    }

    /**
     * 在屏幕空间绘制起点和终点
     */
    private fun drawPointsInScreenSpace(canvas: Canvas) {
        if (trajectoryPoints.isEmpty()) return

        // 起点
        val startPoint = trajectoryPoints.first()
        val startScreenX = offsetX + startPoint.x * scale
        val startScreenY = offsetY - startPoint.y * scale
        canvas.drawCircle(startScreenX, startScreenY, 12f, startPointPaint)

        // 终点（小人图标）
        val endPoint = trajectoryPoints.last()
        val endScreenX = offsetX + endPoint.x * scale
        val endScreenY = offsetY - endPoint.y * scale
        drawPerson(canvas, endScreenX, endScreenY, endPoint.heading)
    }

    /**
     * 绘制小人图标
     * @param canvas 画布
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     * @param heading 航向角（弧度）
     */
    private fun drawPerson(canvas: Canvas, x: Float, y: Float, heading: Float) {
        canvas.save()
        canvas.translate(x, y)
        // 旋转：heading 是从北（上）顺时针的角度，需要转换为屏幕旋转角度
        // 屏幕坐标系：向上是负Y，所以旋转角度需要取负
        canvas.rotate(-Math.toDegrees(heading.toDouble()).toFloat())

        val personPaint = Paint().apply {
            color = Color.parseColor("#FF9800")  // 橙色
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            color = Color.parseColor("#FF9800")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        // 小人尺寸（屏幕像素）
        val headRadius = 8f
        val bodyLength = 20f
        val armLength = 12f
        val legLength = 14f

        // 头（圆形）
        canvas.drawCircle(0f, -bodyLength - headRadius, headRadius, personPaint)

        // 身体（线）
        canvas.drawLine(0f, -bodyLength, 0f, 0f, strokePaint)

        // 手臂（横线）
        canvas.drawLine(-armLength, -bodyLength + 5f, armLength, -bodyLength + 5f, strokePaint)

        // 腿（两条）
        canvas.drawLine(0f, 0f, -8f, legLength, strokePaint)
        canvas.drawLine(0f, 0f, 8f, legLength, strokePaint)

        // 方向指示（前进方向的箭头）
        val arrowPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")  // 绿色
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val arrowLength = 25f
        val arrowWidth = 8f

        // 箭头路径
        val arrowPath = Path()
        arrowPath.moveTo(0f, -bodyLength - headRadius * 2 - 5f)  // 箭头尖端
        arrowPath.lineTo(-arrowWidth, -bodyLength - headRadius * 2 - 5f - arrowLength)  // 左边
        arrowPath.moveTo(0f, -bodyLength - headRadius * 2 - 5f)  // 回到尖端
        arrowPath.lineTo(arrowWidth, -bodyLength - headRadius * 2 - 5f - arrowLength)  // 右边
        canvas.drawPath(arrowPath, arrowPaint)

        canvas.restore()
    }

    private fun drawInfo(canvas: Canvas) {
        if (trajectoryPoints.isEmpty()) return

        val info = buildString {
            append("步数: ${trajectoryPoints.last().stepCount}")
            append("  距离: ${String.format("%.1f", calculateTotalDistance())}m")
        }

        canvas.drawText(info, 20f, 50f, textPaint)

        // 绘制缩放比例
        val scaleInfo = "缩放: ${String.format("%.0f", scale)}px/m"
        canvas.drawText(scaleInfo, 20f, 90f, textPaint)

        // 绘制范围
        val rangeInfo = "范围: ${String.format("%.1f", maxX - minX)}m × ${String.format("%.1f", maxY - minY)}m"
        canvas.drawText(rangeInfo, 20f, 130f, textPaint)
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
        scale = 50f
        autoCenter = true
        invalidate()
    }

    /**
     * 获取轨迹点数量
     */
    fun getPointCount(): Int = trajectoryPoints.size
}