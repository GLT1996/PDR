package com.example.pdr.domain.algorithm

import com.example.pdr.data.model.SensorData
import kotlin.math.sqrt

/**
 * 步数检测器
 * 使用加速度计数据的峰值检测算法识别步伐
 * 支持重力分离和自适应阈值
 */
class StepDetector {

    // 检测参数
    private val windowSize = 15  // 增大窗口以捕获完整步伐周期
    private val minStepInterval = 200L   // 最小步间隔（毫秒）
    private val maxStepInterval = 2000L  // 最大步间隔（毫秒）

    // 低通滤波系数（用于原始数据平滑）
    private val filterAlpha = 0.3f

    // 重力分离参数
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private val gravityAlpha = 0.8f  // 重力滤波系数
    private var gravityInitialized = false

    // 状态变量
    private var lastStepTime = 0L
    private var filteredMagnitude = 0f
    private var initialized = false

    // 滑动窗口用于检测峰值
    private val magnitudeWindow = mutableListOf<Float>()

    // 自适应阈值相关
    private val recentPeakValues = mutableListOf<Float>()
    private val peakHistorySize = 10
    private var adaptiveThresholdFactor = 0.02f  // 初始阈值因子

    // 前后加速度窗口（用于检测前进/后退）
    private val forwardAccelWindow = mutableListOf<Float>()
    private val forwardWindowSize = 25  // 增大窗口

    // 步数计数
    private var stepCount = 0

    // 上一步的方向（1=前进，-1=后退）
    private var lastStepDirection = 1

    // 线性加速度（去除重力后）
    private val linearAcceleration = FloatArray(3)

    /**
     * 处理传感器数据，检测步伐
     * @param sensorData 传感器数据
     * @return StepResult 如果检测到步伐返回步数结果，否则返回null
     */
    fun process(sensorData: SensorData): StepResult? {
        val currentTime = sensorData.timestamp / 1_000_000 // 转换为毫秒
        val acceleration = sensorData.acceleration

        // 分离重力，获取线性加速度
        separateGravity(acceleration)

        // 计算线性加速度模量（去除重力后）
        val linearMagnitude = sqrt(
            linearAcceleration[0] * linearAcceleration[0] +
            linearAcceleration[1] * linearAcceleration[1] +
            linearAcceleration[2] * linearAcceleration[2]
        )

        // 初始化滤波器
        if (!initialized) {
            filteredMagnitude = linearMagnitude
            initialized = true
            return null
        }

        // 低通滤波平滑数据
        filteredMagnitude = filterAlpha * linearMagnitude + (1 - filterAlpha) * filteredMagnitude

        // 更新滑动窗口
        magnitudeWindow.add(filteredMagnitude)
        if (magnitudeWindow.size > windowSize) {
            magnitudeWindow.removeAt(0)
        }

        // 更新前后加速度窗口（使用线性加速度Y分量）
        forwardAccelWindow.add(linearAcceleration[1])
        if (forwardAccelWindow.size > forwardWindowSize) {
            forwardAccelWindow.removeAt(0)
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
     * 分离重力，计算线性加速度
     */
    private fun separateGravity(acceleration: FloatArray) {
        if (!gravityInitialized) {
            gravityX = acceleration[0]
            gravityY = acceleration[1]
            gravityZ = acceleration[2]
            gravityInitialized = true
        } else {
            // 使用低通滤波估计重力分量
            gravityX = gravityAlpha * gravityX + (1 - gravityAlpha) * acceleration[0]
            gravityY = gravityAlpha * gravityY + (1 - gravityAlpha) * acceleration[1]
            gravityZ = gravityAlpha * gravityZ + (1 - gravityAlpha) * acceleration[2]
        }

        // 线性加速度 = 原始加速度 - 重力
        linearAcceleration[0] = acceleration[0] - gravityX
        linearAcceleration[1] = acceleration[1] - gravityY
        linearAcceleration[2] = acceleration[2] - gravityZ
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

        // 计算自适应阈值
        val avg = magnitudeWindow.average().toFloat()
        val threshold = getAdaptiveThreshold(avg)

        // 检查峰值是否足够显著
        if (midValue - avg < threshold) {
            return false
        }

        // 检查时间间隔
        val timeSinceLastStep = currentTime - lastStepTime
        if (lastStepTime > 0 && timeSinceLastStep < minStepInterval) {
            return false
        }

        // 检测到步伐，更新自适应阈值参数
        updateAdaptiveThreshold(midValue, avg)

        stepCount++
        lastStepTime = currentTime
        return true
    }

    /**
     * 获取自适应阈值
     */
    private fun getAdaptiveThreshold(avg: Float): Float {
        return if (recentPeakValues.isNotEmpty()) {
            // 基于历史峰值数据计算阈值
            val recentAvg = recentPeakValues.average().toFloat()
            val threshold = (recentAvg - avg) * 0.5f  // 基于历史峰值高度的50%
            threshold.coerceAtLeast(avg * 0.015f)  // 保证最小阈值
        } else {
            avg * adaptiveThresholdFactor
        }
    }

    /**
     * 更新自适应阈值参数
     */
    private fun updateAdaptiveThreshold(peakValue: Float, avg: Float) {
        recentPeakValues.add(peakValue)
        if (recentPeakValues.size > peakHistorySize) {
            recentPeakValues.removeAt(0)
        }

        // 动态调整阈值因子
        if (recentPeakValues.size >= 5) {
            val avgPeak = recentPeakValues.average().toFloat()
            adaptiveThresholdFactor = (avgPeak - avg) / avg * 0.5f
            adaptiveThresholdFactor = adaptiveThresholdFactor.coerceIn(0.01f, 0.05f)
        }
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

        // 使用自适应阈值
        val directionThreshold = 0.3f  // 降低阈值使其更敏感

        // 如果前半段加速度大于后半段，说明是前进
        // 因为前进时起步阶段向前加速（正值在前），着地时减速（负值在后）
        return if (diff > directionThreshold) {
            1   // 前进
        } else if (diff < -directionThreshold) {
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
        gravityInitialized = false
        gravityX = 0f
        gravityY = 0f
        gravityZ = 0f
        magnitudeWindow.clear()
        forwardAccelWindow.clear()
        recentPeakValues.clear()
        adaptiveThresholdFactor = 0.02f
        lastStepDirection = 1
    }

    /**
     * 步数检测结果
     * @param stepCount 当前总步数
     * @param direction 方向：1=前进，-1=后退
     */
    data class StepResult(val stepCount: Int, val direction: Int)
}