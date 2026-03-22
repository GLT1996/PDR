package com.example.pdr.domain.algorithm

import com.example.pdr.data.model.SensorData
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 步数检测器
 * 使用加速度计数据的峰值检测算法识别步伐
 */
class StepDetector {

    // 检测参数
    private var threshold = 12.0f      // 动态阈值
    private val minThreshold = 10.0f   // 最小阈值
    private val maxThreshold = 15.0f   // 最大阈值

    // 时间窗口参数
    private val minStepInterval = 200L   // 最小步间隔（毫秒），约3步/秒
    private val maxStepInterval = 2000L   // 最大步间隔（毫秒）

    // 低通滤波系数
    private val filterAlpha = 0.3f

    // 状态变量
    private var lastStepTime = 0L
    private var lastAccelMagnitude = 0f
    private var filteredMagnitude = 0f
    private var isPeak = false
    private var isValley = false
    private var lastWasPeak = false

    // 统计数据用于动态阈值调整
    private val magnitudeHistory = mutableListOf<Float>()
    private val historySize = 50

    // 步数计数
    private var stepCount = 0

    /**
     * 处理传感器数据，检测步伐
     * @param sensorData 传感器数据
     * @return 如果检测到步伐返回true
     */
    fun process(sensorData: SensorData): Boolean {
        val currentTime = sensorData.timestamp / 1_000_000 // 转换为毫秒
        val magnitude = sensorData.accelerationMagnitude

        // 低通滤波
        filteredMagnitude = filterAlpha * magnitude + (1 - filterAlpha) * filteredMagnitude

        // 更新历史数据用于动态阈值
        updateMagnitudeHistory(filteredMagnitude)

        // 动态阈值调整
        adjustThreshold()

        // 峰值检测
        val stepDetected = detectStep(filteredMagnitude, currentTime)

        return stepDetected
    }

    /**
     * 峰值检测逻辑
     */
    private fun detectStep(magnitude: Float, currentTime: Long): Boolean {
        // 检测峰值和谷值
        val isCurrentPeak = magnitude > threshold && magnitude < lastAccelMagnitude
        val isCurrentValley = magnitude < threshold && magnitude > lastAccelMagnitude

        var stepDetected = false

        // 检测步伐：从峰值到谷值的过渡
        if (isCurrentValley && lastWasPeak) {
            val timeSinceLastStep = currentTime - lastStepTime

            // 检查时间间隔是否合理
            if (timeSinceLastStep in minStepInterval..maxStepInterval) {
                stepCount++
                lastStepTime = currentTime
                stepDetected = true
            }
        }

        // 更新状态
        lastAccelMagnitude = magnitude
        lastWasPeak = isCurrentPeak

        return stepDetected
    }

    /**
     * 更新历史数据
     */
    private fun updateMagnitudeHistory(magnitude: Float) {
        magnitudeHistory.add(magnitude)
        if (magnitudeHistory.size > historySize) {
            magnitudeHistory.removeAt(0)
        }
    }

    /**
     * 动态调整阈值
     */
    private fun adjustThreshold() {
        if (magnitudeHistory.size < historySize / 2) return

        val avg = magnitudeHistory.average().toFloat()
        val stdDev = calculateStdDev(magnitudeHistory, avg)

        // 基于标准差调整阈值
        threshold = avg + stdDev * 0.5f
        threshold = threshold.coerceIn(minThreshold, maxThreshold)
    }

    /**
     * 计算标准差
     */
    private fun calculateStdDev(data: List<Float>, mean: Float): Float {
        if (data.isEmpty()) return 0f
        val variance = data.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance)
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
        lastAccelMagnitude = 0f
        filteredMagnitude = 0f
        isPeak = false
        isValley = false
        lastWasPeak = false
        magnitudeHistory.clear()
        threshold = 12.0f
    }
}