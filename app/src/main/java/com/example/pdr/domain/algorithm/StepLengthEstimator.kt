package com.example.pdr.domain.algorithm

import kotlin.math.sqrt

/**
 * 步长估计器
 * 使用三模型融合：Weinberg + 步频 + 垂直位移
 */
class StepLengthEstimator {

    // 步长参数
    private var defaultStepLength = 0.70f  // 默认步长（米）

    // Weinberg 步长模型参数
    private var weinbergK = 0.55f  // Weinberg常数（可校准）

    // 动态步长模型参数
    private val minStepLength = 0.45f  // 最小步长
    private val maxStepLength = 0.95f  // 最大步长

    // 加速度历史
    private val accelerationHistory = mutableListOf<Float>()
    private val historySize = 10

    // 步频历史
    private val stepFrequencyHistory = mutableListOf<Float>()
    private var lastStepTime = 0L
    private val frequencyHistorySize = 5

    // 垂直位移模型参数
    private val verticalAccelHistory = mutableListOf<Float>()
    private val verticalHistorySize = 20
    private var lastVerticalVelocity = 0f
    private var lastTimestamp = 0L

    // 用户校准参数
    private var userCalibrated = false
    private var calibrationFactor = 1.0f

    // 最近的加速度振幅（用于校准）
    private val recentAmplitudes = mutableListOf<Float>()
    private val amplitudeHistorySize = 10

    /**
     * 估计步长
     * @param accelerationMagnitude 加速度模量
     * @param stepTime 当前步伐时间戳（纳秒）
     * @param verticalAccel 垂直方向加速度（可选）
     * @return 估计的步长（米）
     */
    fun estimate(accelerationMagnitude: Float, stepTime: Long, verticalAccel: Float = 0f): Float {
        // 更新加速度历史
        updateAccelerationHistory(accelerationMagnitude)

        // 更新步频历史
        updateStepFrequencyHistory(stepTime)

        // 更新垂直加速度历史
        updateVerticalAccelHistory(verticalAccel, stepTime)

        // 三模型融合
        val weinbergStepLength = estimateByWeinberg()
        val frequencyBasedStepLength = estimateByFrequency()
        val verticalStepLength = estimateByVerticalDisplacement()

        // 加权融合：50% Weinberg + 30% 步频 + 20% 垂直位移
        val stepLength = 0.5f * weinbergStepLength +
                         0.3f * frequencyBasedStepLength +
                         0.2f * verticalStepLength

        // 应用用户校准因子
        val calibratedLength = stepLength * calibrationFactor

        return calibratedLength.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 使用固定步长
     */
    fun estimateFixed(): Float {
        return defaultStepLength * calibrationFactor
    }

    /**
     * Weinberg步长模型
     * 基于加速度方差估计步长
     */
    private fun estimateByWeinberg(): Float {
        if (accelerationHistory.isEmpty()) return defaultStepLength

        val maxAccel = accelerationHistory.maxOrNull() ?: return defaultStepLength
        val minAccel = accelerationHistory.minOrNull() ?: return defaultStepLength

        val amplitude = maxAccel - minAccel

        // 记录振幅用于校准
        recentAmplitudes.add(amplitude)
        if (recentAmplitudes.size > amplitudeHistorySize) {
            recentAmplitudes.removeAt(0)
        }

        val stepLength = weinbergK * sqrt(amplitude)

        return stepLength.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 基于步频的步长估计
     * 步频越高，步长通常越大
     */
    private fun estimateByFrequency(): Float {
        if (stepFrequencyHistory.isEmpty()) return defaultStepLength

        val avgFrequency = stepFrequencyHistory.average().toFloat()

        // 改进的线性模型：考虑步频范围
        // 步频1Hz -> 0.55m, 步频2Hz -> 0.70m, 步频3Hz -> 0.85m
        val stepLength = when {
            avgFrequency < 1.5f -> 0.50f + 0.10f * avgFrequency
            avgFrequency < 2.5f -> 0.55f + 0.12f * avgFrequency
            else -> 0.60f + 0.10f * avgFrequency
        }

        return stepLength.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 基于垂直位移的步长估计
     * 垂直位移越大，步长通常越大
     */
    private fun estimateByVerticalDisplacement(): Float {
        if (verticalAccelHistory.isEmpty()) return defaultStepLength

        // 计算垂直加速度的方差（反映身体上下起伏程度）
        val avgVertical = verticalAccelHistory.average().toFloat()
        var variance = 0f
        for (v in verticalAccelHistory) {
            variance += (v - avgVertical) * (v - avgVertical)
        }
        variance /= verticalAccelHistory.size

        // 垂直位移与步长的关系
        // 方差越大，身体起伏越大，步长越长
        val verticalFactor = sqrt(variance) * 0.3f
        val stepLength = defaultStepLength + verticalFactor

        return stepLength.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 更新加速度历史
     */
    private fun updateAccelerationHistory(magnitude: Float) {
        accelerationHistory.add(magnitude)
        if (accelerationHistory.size > historySize) {
            accelerationHistory.removeAt(0)
        }
    }

    /**
     * 更新步频历史
     */
    private fun updateStepFrequencyHistory(stepTime: Long) {
        if (lastStepTime > 0) {
            val intervalMs = (stepTime - lastStepTime) / 1_000_000f
            if (intervalMs > 0) {
                val frequency = 1000f / intervalMs  // Hz
                // 过滤异常频率
                if (frequency in 0.5f..4.0f) {
                    stepFrequencyHistory.add(frequency)
                    if (stepFrequencyHistory.size > frequencyHistorySize) {
                        stepFrequencyHistory.removeAt(0)
                    }
                }
            }
        }
        lastStepTime = stepTime
    }

    /**
     * 更新垂直加速度历史
     */
    private fun updateVerticalAccelHistory(verticalAccel: Float, timestamp: Long) {
        verticalAccelHistory.add(verticalAccel)
        if (verticalAccelHistory.size > verticalHistorySize) {
            verticalAccelHistory.removeAt(0)
        }
        lastTimestamp = timestamp
    }

    /**
     * 用户校准接口
     * 根据已知距离和步数校准步长模型
     * @param knownDistance 已知距离（米）
     * @param steps 走过的步数
     */
    fun calibrateWithKnownDistance(knownDistance: Float, steps: Int) {
        if (steps <= 0 || knownDistance <= 0) return

        val avgStepLength = knownDistance / steps

        // 计算校准因子
        val currentAvgStepLength = if (recentAmplitudes.isNotEmpty()) {
            val avgAmplitude = recentAmplitudes.average().toFloat()
            weinbergK * sqrt(avgAmplitude)
        } else {
            defaultStepLength
        }

        calibrationFactor = avgStepLength / currentAvgStepLength.coerceAtLeast(0.01f)

        // 也可以直接调整 Weinberg K 值
        if (recentAmplitudes.isNotEmpty()) {
            val avgAmplitude = recentAmplitudes.average().toFloat()
            if (avgAmplitude > 0) {
                weinbergK = avgStepLength / sqrt(avgAmplitude)
            }
        }

        userCalibrated = true
    }

    /**
     * 设置默认步长
     */
    fun setDefaultStepLength(length: Float) {
        defaultStepLength = length.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 设置身高（用于调整默认步长）
     * 一般步长约为身高的 0.4-0.45 倍
     */
    fun setUserHeight(heightCm: Float) {
        if (heightCm <= 0) return
        // 步长约为身高的 0.42 倍
        defaultStepLength = (heightCm / 100f) * 0.42f
        defaultStepLength = defaultStepLength.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 是否已校准
     */
    fun isCalibrated(): Boolean = userCalibrated

    /**
     * 获取校准因子
     */
    fun getCalibrationFactor(): Float = calibrationFactor

    /**
     * 重置状态
     */
    fun reset() {
        accelerationHistory.clear()
        stepFrequencyHistory.clear()
        verticalAccelHistory.clear()
        recentAmplitudes.clear()
        lastStepTime = 0L
        lastTimestamp = 0L
        lastVerticalVelocity = 0f
        // 保留校准参数，不重置
    }

    /**
     * 完全重置（包括校准参数）
     */
    fun fullReset() {
        reset()
        weinbergK = 0.55f
        calibrationFactor = 1.0f
        userCalibrated = false
        defaultStepLength = 0.70f
    }
}