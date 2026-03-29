package com.example.pdr.ui.location

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 罗盘视图 - 实时显示设备方向
 */
class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 当前方位角（0-360度，0为正北）
    private var azimuth: Float = 0f

    // 绘制工具
    private val circlePaint: Paint
    private val directionPaint: Paint
    private val arrowPaint: Paint
    private val textPaint: Paint
    private val tickPaint: Paint

    // 罗盘半径
    private var radius: Float = 0f

    init {
        // 圆形边框
        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = 0xFF2196F3.toInt() // 蓝色边框
        }

        // 方向指示文字（N, E, S, W）
        directionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textSize = 36f
            textAlign = Paint.Align.CENTER
            color = 0xFF212121.toInt() // 黑色文字
            isFakeBoldText = true
        }

        // 指针箭头
        arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFFF44336.toInt() // 红色指针（指向北方）
        }

        // 方向数值文字
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textSize = 24f
            textAlign = Paint.Align.CENTER
            color = 0xFF757575.toInt()
        }

        // 刻度线
        tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFF9E9E9E.toInt()
        }
    }

    /**
     * 设置方位角
     * @param azimuth 方位角（0-360度，0为正北）
     */
    fun setAzimuth(azimuth: Float) {
        this.azimuth = azimuth
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 保持正方形
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = minOf(w, h) / 2f * 0.85f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制外圆
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // 绘制内圆（装饰）
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFFBDBDBD.toInt()
        }
        canvas.drawCircle(centerX, centerY, radius * 0.7f, innerPaint)

        // 绘制刻度线（每30度一个主刻度）
        for (i in 0 until 12) {
            val angle = i * 30f
            val isMajor = i % 3 == 0 // 每90度一个大刻度
            val tickLength = if (isMajor) 20f else 10f
            val tickStartRadius = radius - tickLength

            tickPaint.strokeWidth = if (isMajor) 3f else 1.5f
            tickPaint.color = if (isMajor) 0xFF616161.toInt() else 0xFF9E9E9E.toInt()

            val radians = (angle - 90f) * PI / 180f // -90是因为0度在顶部（北方）
            val startX = centerX + tickStartRadius * cos(radians).toFloat()
            val startY = centerY + tickStartRadius * sin(radians).toFloat()
            val endX = centerX + radius * cos(radians).toFloat()
            val endY = centerY + radius * sin(radians).toFloat()

            canvas.drawLine(startX, startY, endX, endY, tickPaint)
        }

        // 绘制方向文字（N, E, S, W）- 这些固定在罗盘上，不随指针旋转
        val directions = arrayOf("N", "E", "S", "W")
        val directionAngles = arrayOf(0f, 90f, 180f, 270f)

        for (i in directions.indices) {
            val angle = directionAngles[i] - 90f // -90是因为屏幕坐标系0度在右侧
            val radians = angle * PI / 180f
            val textRadius = radius * 0.55f
            val x = centerX + textRadius * cos(radians).toFloat()
            val y = centerY + textRadius * sin(radians).toFloat() + directionPaint.textSize / 3

            // 北方特殊颜色
            directionPaint.color = if (i == 0) 0xFFF44336.toInt() else 0xFF212121.toInt()
            canvas.drawText(directions[i], x, y, directionPaint)
        }

        // 绘制指针（指向北方，根据azimuth旋转）
        // 指针始终指向真正的北方，所以需要根据设备方位角旋转
        canvas.save()
        canvas.rotate(-azimuth, centerX, centerY) // 负号：顺时针旋转

        // 绘制指针箭头（红色指向北方）
        val arrowPath = Path()
        val arrowLength = radius * 0.6f
        val arrowWidth = 15f

        // 北方指针（红色，向上）
        arrowPaint.color = 0xFFF44336.toInt()
        arrowPath.moveTo(centerX, centerY - arrowLength) // 顶点
        arrowPath.lineTo(centerX - arrowWidth, centerY)
        arrowPath.lineTo(centerX + arrowWidth, centerY)
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowPaint)

        // 南方指针（白色/浅色，向下）
        arrowPaint.color = 0xFFFFFFFF.toInt()
        arrowPaint.style = Paint.Style.FILL_AND_STROKE
        val southPath = Path()
        val southLength = radius * 0.35f
        southPath.moveTo(centerX, centerY + southLength)
        southPath.lineTo(centerX - arrowWidth * 0.7f, centerY)
        southPath.lineTo(centerX + arrowWidth * 0.7f, centerY)
        southPath.close()
        canvas.drawPath(southPath, arrowPaint)

        // 南方指针边框
        val southBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFF9E9E9E.toInt()
        }
        canvas.drawPath(southPath, southBorderPaint)

        canvas.restore()

        // 绘制中心圆点
        val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFF37474F.toInt()
        }
        canvas.drawCircle(centerX, centerY, 8f, centerDotPaint)

        // 绘制底部方位角数值
        val azimuthText = String.format("%.1f°", azimuth)
        val directionName = getDirectionName(azimuth)
        canvas.drawText("$directionName $azimuthText", centerX, centerY + radius + 40f, textPaint)
    }

    /**
     * 根据方位角返回方向名称
     */
    private fun getDirectionName(azimuth: Float): String {
        val normalizedAzimuth = ((azimuth % 360) + 360) % 360

        return when {
            normalizedAzimuth < 22.5 || normalizedAzimuth >= 337.5 -> "北"
            normalizedAzimuth < 67.5 -> "东北"
            normalizedAzimuth < 112.5 -> "东"
            normalizedAzimuth < 157.5 -> "东南"
            normalizedAzimuth < 202.5 -> "南"
            normalizedAzimuth < 247.5 -> "西南"
            normalizedAzimuth < 292.5 -> "西"
            else -> "西北"
        }
    }
}