package com.example.pdr.data.repository

import com.example.pdr.data.model.SensorData
import com.example.pdr.data.model.TrajectoryPoint
import com.example.pdr.domain.algorithm.PositionCalculator
import com.example.pdr.domain.sensor.SensorController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * PDR数据仓库
 * 封装传感器数据处理逻辑，提供位置更新流
 */
class PDRRepository(private val sensorController: SensorController) {

    private val positionCalculator = PositionCalculator()

    // 状态
    private var isRecording = false
    private var currentTrajectoryId: Long = 0

    /**
     * 检查传感器是否可用
     */
    fun hasRequiredSensors(): Boolean {
        return sensorController.hasRequiredSensors()
    }

    /**
     * 获取传感器可用性信息
     */
    fun getSensorAvailability(): Map<String, Boolean> {
        return sensorController.getSensorAvailability()
    }

    /**
     * 获取位置更新流
     * 每次检测到步伐并更新位置时发出新的轨迹点
     */
    fun getPositionUpdates(): Flow<TrajectoryPoint> {
        return sensorController.getSensorDataFlow()
            .map { sensorData ->
                if (isRecording) {
                    positionCalculator.process(sensorData)
                } else {
                    null
                }
            }
            .filterNotNull()
    }

    /**
     * 获取传感器数据流（原始）
     */
    fun getSensorDataStream(): Flow<SensorData> {
        return sensorController.getSensorDataFlow()
    }

    /**
     * 开始记录轨迹
     */
    fun startRecording(trajectoryId: Long = 0) {
        currentTrajectoryId = trajectoryId
        positionCalculator.setTrajectoryId(trajectoryId)
        positionCalculator.start()
        isRecording = true
    }

    /**
     * 停止记录轨迹
     */
    fun stopRecording() {
        isRecording = false
    }

    /**
     * 重置位置计算器
     */
    fun resetPosition() {
        positionCalculator.reset()
    }

    /**
     * 校准航向
     */
    fun calibrateHeading() {
        positionCalculator.calibrateHeading()
    }

    /**
     * 获取当前位置
     */
    fun getCurrentPosition(): Pair<Float, Float> {
        return positionCalculator.getCurrentPosition()
    }

    /**
     * 获取当前航向
     */
    fun getCurrentHeading(): Float {
        return positionCalculator.getCurrentHeading()
    }

    /**
     * 获取总步数
     */
    fun getTotalSteps(): Int {
        return positionCalculator.getTotalSteps()
    }

    /**
     * 获取总距离
     */
    fun getTotalDistance(): Float {
        return positionCalculator.getTotalDistance()
    }

    /**
     * 获取记录时长
     */
    fun getDuration(): Long {
        return positionCalculator.getDuration()
    }

    /**
     * 获取所有轨迹点
     */
    fun getTrajectoryPoints(): List<TrajectoryPoint> {
        return positionCalculator.getTrajectoryPoints()
    }

    /**
     * 是否正在记录
     */
    fun isRecording(): Boolean = isRecording
}