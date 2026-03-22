package com.example.pdr.domain.algorithm

import com.example.pdr.data.model.SensorData
import com.example.pdr.data.model.TrajectoryPoint

/**
 * 位置计算器
 * 根据步长和航向计算移动位置
 * 支持位置平滑滤波
 */
class PositionCalculator {

    // 当前位置
    private var currentX = 0f
    private var currentY = 0f

    // 平滑后的位置
    private var smoothedX = 0f
    private var smoothedY = 0f

    // 统计信息
    private var totalSteps = 0
    private var totalDistance = 0f
    private var startTime = 0L

    // 轨迹点列表
    private val trajectoryPoints = mutableListOf<TrajectoryPoint>()

    // 子模块
    private val stepDetector = StepDetector()
    private val stepLengthEstimator = StepLengthEstimator()
    private val headingEstimator = HeadingEstimator()

    // 当前轨迹ID
    private var currentTrajectoryId: Long = 0

    // 位置平滑相关
    private val recentPositions = mutableListOf<Pair<Float, Float>>()
    private var smoothingWindow = 3  // 平滑窗口大小
    private var smoothingEnabled = true

    /**
     * 处理传感器数据，更新位置
     * @param sensorData 传感器数据
     * @return 如果检测到步伐并更新了位置返回新的轨迹点，否则返回null
     */
    fun process(sensorData: SensorData): TrajectoryPoint? {
        // 更新航向
        val heading = headingEstimator.process(sensorData)

        // 检测步伐
        val stepResult = stepDetector.process(sensorData)

        if (stepResult != null) {
            // 获取垂直加速度
            val verticalAccel = sensorData.getVerticalAcceleration()

            // 估计步长（使用三模型融合）
            val stepLength = stepLengthEstimator.estimate(
                sensorData.accelerationMagnitude,
                sensorData.timestamp,
                verticalAccel
            )

            // 更新位置（使用方向）
            updatePosition(stepLength, heading, stepResult.direction)

            // 应用位置平滑
            val (finalX, finalY) = if (smoothingEnabled) {
                smoothPosition(currentX, currentY)
            } else {
                Pair(currentX, currentY)
            }

            // 创建轨迹点（使用平滑后的位置）
            val point = TrajectoryPoint(
                trajectoryId = currentTrajectoryId,
                timestamp = sensorData.timestamp,
                x = finalX,
                y = finalY,
                heading = heading,
                stepCount = totalSteps
            )

            trajectoryPoints.add(point)
            return point
        }

        return null
    }

    /**
     * 更新位置坐标
     */
    private fun updatePosition(stepLength: Float, heading: Float, direction: Int) {
        // Android方位角定义：正北为0，顺时针增加
        // 东 = π/2, 南 = π, 西 = -π/2
        //
        // direction: 1=前进, -1=后退

        val effectiveStepLength = stepLength * direction

        val dx = effectiveStepLength * kotlin.math.sin(heading)   // 东（右）为正
        val dy = effectiveStepLength * kotlin.math.cos(heading)   // 北（上）为正

        currentX += dx
        currentY += dy

        totalSteps++
        totalDistance += stepLength
    }

    /**
     * 位置平滑滤波（移动平均）
     * 减少轨迹抖动，使轨迹更平滑
     */
    private fun smoothPosition(x: Float, y: Float): Pair<Float, Float> {
        recentPositions.add(Pair(x, y))

        if (recentPositions.size > smoothingWindow) {
            recentPositions.removeAt(0)
        }

        // 计算移动平均
        val avgX = recentPositions.map { it.first }.average().toFloat()
        val avgY = recentPositions.map { it.second }.average().toFloat()

        smoothedX = avgX
        smoothedY = avgY

        return Pair(avgX, avgY)
    }

    /**
     * 设置是否启用位置平滑
     */
    fun setSmoothingEnabled(enabled: Boolean) {
        smoothingEnabled = enabled
    }

    /**
     * 设置平滑窗口大小
     */
    fun setSmoothingWindow(size: Int) {
        smoothingWindow = size.coerceIn(1, 5)
    }

    /**
     * 获取当前位置
     */
    fun getCurrentPosition(): Pair<Float, Float> = Pair(currentX, currentY)

    /**
     * 获取当前航向
     */
    fun getCurrentHeading(): Float = headingEstimator.getHeading()

    /**
     * 获取总步数
     */
    fun getTotalSteps(): Int = totalSteps

    /**
     * 获取总距离
     */
    fun getTotalDistance(): Float = totalDistance

    /**
     * 获取所有轨迹点
     */
    fun getTrajectoryPoints(): List<TrajectoryPoint> = trajectoryPoints.toList()

    /**
     * 获取轨迹点数量
     */
    fun getPointCount(): Int = trajectoryPoints.size

    /**
     * 设置初始位置
     */
    fun setInitialPosition(x: Float, y: Float) {
        currentX = x
        currentY = y
    }

    /**
     * 设置轨迹ID
     */
    fun setTrajectoryId(id: Long) {
        currentTrajectoryId = id
    }

    /**
     * 设置初始方向校准
     */
    fun calibrateHeading() {
        headingEstimator.calibrate()
    }

    /**
     * 开始记录
     */
    fun start() {
        startTime = System.currentTimeMillis()
    }

    /**
     * 获取记录时长（毫秒）
     */
    fun getDuration(): Long {
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime
        } else {
            0
        }
    }

    /**
     * 重置计算器状态
     */
    fun reset() {
        currentX = 0f
        currentY = 0f
        smoothedX = 0f
        smoothedY = 0f
        totalSteps = 0
        totalDistance = 0f
        startTime = 0L
        currentTrajectoryId = 0
        trajectoryPoints.clear()
        recentPositions.clear()
        stepDetector.reset()
        stepLengthEstimator.reset()
        headingEstimator.reset()
    }

    /**
     * 清除轨迹点但保留累计数据
     */
    fun clearPoints() {
        trajectoryPoints.clear()
    }
}