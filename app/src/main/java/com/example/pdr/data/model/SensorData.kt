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