package com.example.pdr.data.model

/**
 * 传感器数据封装类
 * 用于封装从手机传感器获取的原始数据
 */
data class SensorData(
    val timestamp: Long,
    val acceleration: FloatArray,
    val gyroscope: FloatArray? = null,
    val magneticField: FloatArray? = null,
    val rotationVector: FloatArray? = null
) {
    // 加速度模量（用于步数检测）
    val accelerationMagnitude: Float
        get() = kotlin.math.sqrt(
            acceleration[0] * acceleration[0] +
            acceleration[1] * acceleration[1] +
            acceleration[2] * acceleration[2]
        )

    /**
     * 获取垂直方向加速度（基于设备姿态）
     * 使用 RotationVector 计算设备姿态后，提取垂直方向（世界坐标系的Z轴）的加速度
     * 用于步长估计中的垂直位移模型
     */
    fun getVerticalAcceleration(): Float {
        rotationVector ?: return 0f

        // 从旋转向量计算设备相对于世界坐标系的姿态
        // 简化计算：假设加速度的垂直分量主要来自 Z 轴
        // 更精确的计算需要完整的旋转矩阵变换

        // 使用简单的重力分离方法估计垂直加速度
        // 这里返回加速度在垂直方向的投影
        val gravityNorm = kotlin.math.sqrt(
            acceleration[0] * acceleration[0] +
            acceleration[1] * acceleration[1] +
            acceleration[2] * acceleration[2]
        )

        if (gravityNorm < 0.1f) return 0f

        // 加速度向量归一化后的垂直分量
        // 假设重力方向约为 (0, 0, 1)，计算加速度的垂直投影
        return acceleration[2] - 9.81f * (acceleration[2] / gravityNorm)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorData
        if (timestamp != other.timestamp) return false
        if (!acceleration.contentEquals(other.acceleration)) return false
        if (gyroscope != null) {
            if (other.gyroscope == null) return false
            if (!gyroscope.contentEquals(other.gyroscope)) return false
        } else if (other.gyroscope != null) return false
        if (magneticField != null) {
            if (other.magneticField == null) return false
            if (!magneticField.contentEquals(other.magneticField)) return false
        } else if (other.magneticField != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + acceleration.contentHashCode()
        result = 31 * result + (gyroscope?.contentHashCode() ?: 0)
        result = 31 * result + (magneticField?.contentHashCode() ?: 0)
        return result
    }
}