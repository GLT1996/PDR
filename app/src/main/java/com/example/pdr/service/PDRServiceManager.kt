package com.example.pdr.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.pdr.data.model.SensorData
import com.example.pdr.data.model.TrajectoryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * PDR服务管理器
 * 管理前台服务的启动、绑定和通信
 */
class PDRServiceManager(private val context: Context) {

    private var service: PDRService? = null
    private var isBound = false

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps

    private val _currentDistance = MutableStateFlow(0f)
    val currentDistance: StateFlow<Float> = _currentDistance

    private val _currentDuration = MutableStateFlow(0L)
    val currentDuration: StateFlow<Long> = _currentDuration

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as PDRService.LocalBinder
            service = localBinder.getService()
            isBound = true

            // 设置回调
            service?.setOnPositionUpdateListener { _ ->
                _currentSteps.value = service?.getTotalSteps() ?: 0
                _currentDistance.value = service?.getTotalDistance() ?: 0f
                _currentDuration.value = service?.getDuration() ?: 0
            }

            _isRunning.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isRunning.value = false
        }
    }

    /**
     * 启动并绑定服务
     */
    fun startAndBind(trajectoryId: Long = 0) {
        val intent = Intent(context, PDRService::class.java).apply {
            action = PDRService.ACTION_START
            putExtra(PDRService.EXTRA_TRAJECTORY_ID, trajectoryId)
        }

        // 启动前台服务
        context.startForegroundService(intent)

        // 绑定服务
        context.bindService(
            Intent(context, PDRService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * 停止服务
     */
    fun stop() {
        val intent = Intent(context, PDRService::class.java).apply {
            action = PDRService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * 暂停记录
     */
    fun pause() {
        val intent = Intent(context, PDRService::class.java).apply {
            action = PDRService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    /**
     * 恢复记录
     */
    fun resume() {
        val intent = Intent(context, PDRService::class.java).apply {
            action = PDRService.ACTION_RESUME
        }
        context.startService(intent)
    }

    /**
     * 解绑服务
     */
    fun unbind() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * 设置位置更新监听器
     */
    fun setOnPositionUpdateListener(listener: (TrajectoryPoint) -> Unit) {
        service?.setOnPositionUpdateListener(listener)
    }

    /**
     * 设置传感器更新监听器
     */
    fun setOnSensorUpdateListener(listener: (SensorData) -> Unit) {
        service?.setOnSensorUpdateListener(listener)
    }

    /**
     * 获取总步数
     */
    fun getTotalSteps(): Int = service?.getTotalSteps() ?: 0

    /**
     * 获取总距离
     */
    fun getTotalDistance(): Float = service?.getTotalDistance() ?: 0f

    /**
     * 获取记录时长
     */
    fun getDuration(): Long = service?.getDuration() ?: 0

    /**
     * 获取当前位置
     */
    fun getCurrentPosition(): Pair<Float, Float> = service?.getCurrentPosition() ?: Pair(0f, 0f)

    /**
     * 获取当前航向
     */
    fun getCurrentHeading(): Float = service?.getCurrentHeading() ?: 0f

    /**
     * 获取轨迹点列表
     */
    fun getTrajectoryPoints(): List<TrajectoryPoint> = service?.getTrajectoryPoints() ?: emptyList()

    /**
     * 校准航向
     */
    fun calibrateHeading() {
        service?.calibrateHeading()
    }

    /**
     * 检查服务是否正在运行
     */
    fun isServiceRunning(): Boolean = PDRService.isRunning()
}