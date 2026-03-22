package com.example.pdr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.pdr.MainActivity
import com.example.pdr.R
import com.example.pdr.data.model.TrajectoryPoint
import com.example.pdr.data.repository.PDRRepository
import com.example.pdr.domain.sensor.SensorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * PDR前台服务
 * 在后台持续运行传感器监听，支持息屏后继续记录轨迹
 */
class PDRService : Service() {

    companion object {
        const val CHANNEL_ID = "pdr_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.pdr.action.START"
        const val ACTION_STOP = "com.example.pdr.action.STOP"
        const val ACTION_PAUSE = "com.example.pdr.action.PAUSE"
        const val ACTION_RESUME = "com.example.pdr.action.RESUME"

        const val EXTRA_TRAJECTORY_ID = "trajectory_id"

        private var isRunning = false

        fun isRunning(): Boolean = isRunning
    }

    private val binder = LocalBinder()
    private var pdrRepository: PDRRepository? = null
    private var sensorJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    // 状态
    private var isRecording = false
    private var currentTrajectoryId: Long = 0

    // 回调
    private var onPositionUpdate: ((TrajectoryPoint) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): PDRService = this@PDRService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initRepository()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val trajectoryId = intent.getLongExtra(EXTRA_TRAJECTORY_ID, 0)
                startTracking(trajectoryId)
            }
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
            }
            ACTION_PAUSE -> {
                pauseTracking()
            }
            ACTION_RESUME -> {
                resumeTracking()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorTracking()
        isRunning = false
    }

    private fun initRepository() {
        val sensorController = SensorController(applicationContext)
        pdrRepository = PDRRepository(sensorController)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PDR轨迹记录",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台持续记录行走轨迹"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val steps = pdrRepository?.getTotalSteps() ?: 0
        val distance = pdrRepository?.getTotalDistance() ?: 0f
        val duration = formatDuration(pdrRepository?.getDuration() ?: 0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在记录轨迹")
            .setContentText("步数: $steps | 距离: ${String.format("%.1f", distance)}m | 时长: $duration")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startTracking(trajectoryId: Long) {
        currentTrajectoryId = trajectoryId
        isRecording = true
        isRunning = true

        // 重置状态（清除上次的轨迹数据）
        pdrRepository?.resetPosition()

        // 启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // 开始传感器追踪
        startSensorTracking()
    }

    private fun startSensorTracking() {
        // 先取消之前的传感器监听
        stopSensorTracking()

        pdrRepository?.startRecording(currentTrajectoryId)

        sensorJob = serviceScope.launch {
            pdrRepository?.getPositionUpdates()
                ?.onEach { point ->
                    onPositionUpdate?.invoke(point)
                    updateNotification()
                }
                ?.launchIn(this)
        }
    }

    private fun stopSensorTracking() {
        sensorJob?.cancel()
        sensorJob = null
        pdrRepository?.stopRecording()
    }

    private fun stopTracking() {
        stopSensorTracking()
        pdrRepository?.resetPosition()
        isRecording = false
        isRunning = false
    }

    private fun pauseTracking() {
        pdrRepository?.stopRecording()
        isRecording = false
    }

    private fun resumeTracking() {
        pdrRepository?.startRecording(currentTrajectoryId)
        isRecording = true
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // ============ 公共API ============

    fun setOnPositionUpdateListener(listener: (TrajectoryPoint) -> Unit) {
        onPositionUpdate = listener
    }

    fun getTotalSteps(): Int = pdrRepository?.getTotalSteps() ?: 0

    fun getTotalDistance(): Float = pdrRepository?.getTotalDistance() ?: 0f

    fun getDuration(): Long = pdrRepository?.getDuration() ?: 0

    fun getCurrentPosition(): Pair<Float, Float> = pdrRepository?.getCurrentPosition() ?: Pair(0f, 0f)

    fun getCurrentHeading(): Float = pdrRepository?.getCurrentHeading() ?: 0f

    fun getTrajectoryPoints(): List<TrajectoryPoint> = pdrRepository?.getTrajectoryPoints() ?: emptyList()

    fun calibrateHeading() {
        pdrRepository?.calibrateHeading()
    }

    fun resetPosition() {
        pdrRepository?.resetPosition()
    }
}