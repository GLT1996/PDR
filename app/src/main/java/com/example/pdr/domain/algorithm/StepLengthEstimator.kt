package com.example.pdr.domain.algorithm

import kotlin.math.sqrt

/**
 * 步长估计器
 * 根据行走特征估计每步的长度
 */
class StepLengthEstimator {

    // 步长参数
    private var defaultStepLength = 0.70f  // 默认步长（米）

    // Weinberg 步长模型参数
    private val weinbergK = 0.55f  // Weinberg常数

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

    /**
     * 估计步长
     * @param accelerationMagnitude 加速度模量
     * @param stepTime 当前步伐时间戳（纳秒）
     * @return 估计的步长（米）
     */
    fun estimate(accelerationMagnitude: Float, stepTime: Long): Float {
        // 更新加速度历史
        updateAccelerationHistory(accelerationMagnitude)

        // 更新步频历史
        updateStepFrequencyHistory(stepTime)

        // 使用混合模型估计步长
        val weinbergStepLength = estimateByWeinberg()
        val frequencyBasedStepLength = estimateByFrequency()

        // 加权融合
        val stepLength = 0.6f * weinbergStepLength + 0.4f * frequencyBasedStepLength

        return stepLength.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 使用固定步长
     */
    fun estimateFixed(): Float {
        return defaultStepLength
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

        // 线性模型：步长与步频正相关
        // 步频1Hz -> 0.55m, 步频2Hz -> 0.75m, 步频3Hz -> 0.85m
        val stepLength = 0.45f + 0.15f * avgFrequency

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
                stepFrequencyHistory.add(frequency)
                if (stepFrequencyHistory.size > frequencyHistorySize) {
                    stepFrequencyHistory.removeAt(0)
                }
            }
        }
        lastStepTime = stepTime
    }

    /**
     * 设置默认步长
     */
    fun setDefaultStepLength(length: Float) {
        defaultStepLength = length.coerceIn(minStepLength, maxStepLength)
    }

    /**
     * 重置状态
     */
    fun reset() {
        accelerationHistory.clear()
        stepFrequencyHistory.clear()
        lastStepTime = 0L
    }
}