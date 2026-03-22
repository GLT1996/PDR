package com.example.pdr.domain.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.pdr.data.model.SensorData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 传感器控制器
 * 负责管理手机传感器的注册、数据采集和注销
 */
class SensorController(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // 传感器引用
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // 采样率（微秒）
    private val samplingPeriodUs = 20000 // 50Hz

    /**
     * 检查必要传感器是否可用
     */
    fun hasRequiredSensors(): Boolean {
        return accelerometer != null
    }

    /**
     * 获取传感器可用性信息
     */
    fun getSensorAvailability(): Map<String, Boolean> {
        return mapOf(
            "Accelerometer" to (accelerometer != null),
            "Gyroscope" to (gyroscope != null),
            "Magnetometer" to (magnetometer != null),
            "RotationVector" to (rotationVector != null)
        )
    }

    /**
     * 获取传感器数据流
     * 返回融合后的传感器数据
     */
    fun getSensorDataFlow(): Flow<SensorData> = callbackFlow {
        val listener = object : SensorEventListener {
            // 缓存最新的传感器数据
            private var lastAcceleration: FloatArray? = null
            private var lastGyroscope: FloatArray? = null
            private var lastMagneticField: FloatArray? = null
            private var lastRotationVector: FloatArray? = null
            private var lastTimestamp: Long = 0

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        lastAcceleration = event.values.clone()
                        lastTimestamp = event.timestamp
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        lastGyroscope = event.values.clone()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lastMagneticField = event.values.clone()
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        lastRotationVector = event.values.clone()
                    }
                }

                // 当有加速度数据时发送融合数据
                lastAcceleration?.let { acc ->
                    val sensorData = SensorData(
                        timestamp = lastTimestamp,
                        acceleration = acc,
                        gyroscope = lastGyroscope,
                        magneticField = lastMagneticField,
                        rotationVector = lastRotationVector
                    )
                    trySend(sensorData)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // 可以在这里处理传感器精度变化
            }
        }

        // 注册传感器监听
        accelerometer?.let {
            sensorManager.registerListener(listener, it, samplingPeriodUs)
        }
        gyroscope?.let {
            sensorManager.registerListener(listener, it, samplingPeriodUs)
        }
        magnetometer?.let {
            sensorManager.registerListener(listener, it, samplingPeriodUs)
        }
        rotationVector?.let {
            sensorManager.registerListener(listener, it, samplingPeriodUs)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * 获取加速度计数据流（单独）
     */
    fun getAccelerometerFlow(): Flow<SensorData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val sensorData = SensorData(
                        timestamp = event.timestamp,
                        acceleration = event.values.clone()
                    )
                    trySend(sensorData)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, samplingPeriodUs)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    companion object {
        private const val TAG = "SensorController"
    }
}