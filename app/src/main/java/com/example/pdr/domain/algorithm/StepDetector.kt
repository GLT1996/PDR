package com.example.pdr.domain.algorithm

import com.example.pdr.data.model.SensorData

/**
 * 步数检测器
 * 使用加速度计数据的峰值检测算法识别步伐
 * 同时检测前进/后退方向
 */
class StepDetector {

    // 检测参数
    private val windowSize = 10
    private val minStepInterval = 200L   // 最小步间隔（毫秒）
    private val maxStepInterval = 2000L  // 最大步间隔（毫秒）

    // 低通滤波系数
    private val filterAlpha = 0.4f

    // 状态变量
    private var lastStepTime = 0L
    private var filteredMagnitude = 0f
    private var initialized = false

    // 滑动窗口用于检测峰值
    private val magnitudeWindow = mutableListOf<Float>()

    // 前后加速度窗口（用于检测前进/后退）
    private val forwardAccelWindow = mutableListOf<Float>()
    private val forwardWindowSize = 20

    // 步数计数
    private var stepCount = 0

    // 上一步的方向（1=前进，-1=后退）
    private var lastStepDirection = 1

    /**
     * 处理传感器数据，检测步伐
     * @param sensorData 传感器数据
     * @return StepResult 如果检测到步伐返回步数结果，否则返回null
     */
    fun process(sensorData: SensorData): StepResult? {
        val currentTime = sensorData.timestamp / 1_000_000 // 转换为毫秒
        val magnitude = sensorData.accelerationMagnitude

        // 初始化滤波器
        if (!initialized) {
            filteredMagnitude = magnitude
            initialized = true
            return null
        }

        // 低通滤波
        filteredMagnitude = filterAlpha * magnitude + (1 - filterAlpha) * filteredMagnitude

        // 更新滑动窗口
        magnitudeWindow.add(filteredMagnitude)
        if (magnitudeWindow.size > windowSize) {
            magnitudeWindow.removeAt(0)
        }

        // 更新前后加速度窗口（使用设备Y轴加速度作为前后方向）
        // 注意：手机正常手持时，设备Y轴指向前方
        sensorData.acceleration.getOrNull(1)?.let { forwardAccel ->
            forwardAccelWindow.add(forwardAccel)
            if (forwardAccelWindow.size > forwardWindowSize) {
                forwardAccelWindow.removeAt(0)
            }
        }

        // 需要足够的数据才能检测
        if (magnitudeWindow.size < windowSize) {
            return null
        }

        // 峰值检测
        val stepDetected = detectPeak(currentTime)

        return if (stepDetected) {
            // 检测方向
            val direction = detectDirection()
            StepResult(stepCount, direction)
        } else {
            null
        }
    }

    /**
     * 峰值检测逻辑
     */
    private fun detectPeak(currentTime: Long): Boolean {
        val midIndex = windowSize / 2
        val midValue = magnitudeWindow[midIndex]

        // 检查中间值是否是局部最大值
        var isPeak = true
        for (i in magnitudeWindow.indices) {
            if (i != midIndex && magnitudeWindow[i] >= midValue) {
                isPeak = false
                break
            }
        }

        if (!isPeak) return false

        // 检查峰值是否足够显著
        val avg = magnitudeWindow.average().toFloat()
        val peakThreshold = avg * 0.02f

        if (midValue - avg < peakThreshold) {
            return false
        }

        // 检查时间间隔
        val timeSinceLastStep = currentTime - lastStepTime
        if (lastStepTime > 0 && timeSinceLastStep < minStepInterval) {
            return false
        }

        // 检测到步伐
        stepCount++
        lastStepTime = currentTime
        return true
    }

    /**
     * 检测步伐方向（前进或后退）
     * 原理：分析步伐周期内的前后加速度变化模式
     * 前进：起步时向前加速（正Y），着地时向后减速（负Y）
     * 后退：起步时向后加速（负Y），着地时向前减速（正Y）
     */
    private fun detectDirection(): Int {
        if (forwardAccelWindow.size < forwardWindowSize) {
            return 1  // 默认前进
        }

        // 分析加速度变化：前半段减去后半段
        val halfSize = forwardWindowSize / 2
        var firstHalfSum = 0f
        var secondHalfSum = 0f

        for (i in 0 until halfSize) {
            firstHalfSum += forwardAccelWindow[i]
        }
        for (i in halfSize until forwardWindowSize) {
            secondHalfSum += forwardAccelWindow[i]
        }

        val diff = firstHalfSum - secondHalfSum

        // 如果前半段加速度大于后半段，说明是前进
        // 因为前进时起步阶段向前加速（正值在前），着地时减速（负值在后）
        return if (diff > 0.5f) {
            1   // 前进
        } else if (diff < -0.5f) {
            -1  // 后退
        } else {
            lastStepDirection  // 保持上一次的方向
        }.also {
            lastStepDirection = it
        }
    }

    /**
     * 获取当前步数
     */
    fun getStepCount(): Int = stepCount

    /**
     * 重置检测器状态
     */
    fun reset() {
        stepCount = 0
        lastStepTime = 0L
        filteredMagnitude = 0f
        initialized = false
        magnitudeWindow.clear()
        forwardAccelWindow.clear()
        lastStepDirection = 1
    }

    /**
     * 步数检测结果
     * @param stepCount 当前总步数
     * @param direction 方向：1=前进，-1=后退
     */
    data class StepResult(val stepCount: Int, val direction: Int)
}