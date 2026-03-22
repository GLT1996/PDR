package com.example.pdr.domain.algorithm

import android.hardware.SensorManager
import com.example.pdr.data.model.SensorData
import kotlin.math.abs
import kotlin.math.atan2

/**
 * 航向估计器
 * 融合陀螺仪和磁力计数据估计行走方向
 */
class HeadingEstimator {

    // 互补滤波系数（陀螺仪权重）
    private val gyroWeight = 0.98f
    private val magWeight = 0.02f

    // 当前航向角（弧度）
    private var currentHeading = 0f

    // 陀螺仪积分航向
    private var gyroHeading = 0f

    // 磁力计航向
    private var magneticHeading = 0f

    // 上一次的时间戳
    private var lastTimestamp = 0L

    // 旋转矩阵
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // 校准相关
    private var isCalibrated = false
    private var headingOffset = 0f

    /**
     * 处理传感器数据，更新航向估计
     * @param sensorData 传感器数据
     * @return 当前航向角（弧度，已校准）
     */
    fun process(sensorData: SensorData): Float {
        val timestamp = sensorData.timestamp

        // 更新陀螺仪航向
        sensorData.gyroscope?.let { updateGyroHeading(it, timestamp) }

        // 更新磁力计航向
        sensorData.magneticField?.let { updateMagneticHeading(sensorData.acceleration, it) }

        // 互补滤波融合
        fuseHeadings()

        lastTimestamp = timestamp
        // 返回校准后的航向
        return getCalibratedHeading()
    }

    /**
     * 使用陀螺仪更新航向
     */
    private fun updateGyroHeading(gyro: FloatArray, timestamp: Long) {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            return
        }

        val dt = (timestamp - lastTimestamp) / 1_000_000_000f  // 转换为秒

        // 陀螺仪z轴角速度积分（绕垂直轴旋转）
        // 注意：Android陀螺仪Z轴旋转正方向是逆时针（从Z轴正向看）
        // 而我们定义的航向正方向是顺时针（东为正角度变化）
        // 所以需要取反
        gyroHeading -= gyro[2] * dt

        // 归一化到 [-π, π]
        gyroHeading = normalizeAngle(gyroHeading)
    }

    /**
     * 使用磁力计更新航向
     */
    private fun updateMagneticHeading(acceleration: FloatArray, magnetic: FloatArray) {
        // 计算设备方向
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            acceleration,
            magnetic
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            magneticHeading = normalizeAngle(orientationAngles[0])
        }
    }

    /**
     * 互补滤波融合陀螺仪和磁力计航向
     */
    private fun fuseHeadings() {
        // 处理角度跳变问题（例如从179度到-179度）
        val angleDiff = normalizeAngle(magneticHeading - gyroHeading)

        // 互补滤波
        currentHeading = gyroHeading + magWeight * angleDiff
        currentHeading = normalizeAngle(currentHeading)

        // 缓慢校正陀螺仪漂移
        gyroHeading += 0.01f * angleDiff
        gyroHeading = normalizeAngle(gyroHeading)
    }

    /**
     * 归一化角度到 [-π, π]
     */
    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle
        while (normalized > Math.PI) normalized -= 2 * Math.PI.toFloat()
        while (normalized < -Math.PI) normalized += 2 * Math.PI.toFloat()
        return normalized
    }

    /**
     * 获取当前航向（弧度，已校准）
     */
    fun getHeading(): Float = getCalibratedHeading()

    /**
     * 获取当前航向（度）
     */
    fun getHeadingDegrees(): Float = Math.toDegrees(currentHeading.toDouble()).toFloat()

    /**
     * 校准航向（设置当前方向为参考方向）
     */
    fun calibrate() {
        headingOffset = -currentHeading
        isCalibrated = true
    }

    /**
     * 获取校准后的航向
     */
    fun getCalibratedHeading(): Float {
        return normalizeAngle(currentHeading + headingOffset)
    }

    /**
     * 重置状态
     */
    fun reset() {
        currentHeading = 0f
        gyroHeading = 0f
        magneticHeading = 0f
        lastTimestamp = 0L
        headingOffset = 0f
        isCalibrated = false
    }

    companion object {
        private const val TAG = "HeadingEstimator"
    }
}