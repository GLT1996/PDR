package com.example.pdr.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdr.data.local.TrajectoryDatabase
import com.example.pdr.data.model.Trajectory
import com.example.pdr.data.model.TrajectoryPoint
import com.example.pdr.data.repository.PDRRepository
import com.example.pdr.data.repository.TrajectoryRepository
import com.example.pdr.domain.sensor.SensorController
import com.example.pdr.service.PDRServiceManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private lateinit var pdrRepository: PDRRepository
    private lateinit var trajectoryRepository: TrajectoryRepository
    private lateinit var serviceManager: PDRServiceManager

    // 状态
    private val _isRecording = MutableLiveData<Boolean>()
    val isRecording: LiveData<Boolean> = _isRecording

    private val _currentPosition = MutableLiveData<Pair<Float, Float>>()
    val currentPosition: LiveData<Pair<Float, Float>> = _currentPosition

    private val _currentHeading = MutableLiveData<Float>()
    val currentHeading: LiveData<Float> = _currentHeading

    private val _totalSteps = MutableLiveData<Int>()
    val totalSteps: LiveData<Int> = _totalSteps

    private val _totalDistance = MutableLiveData<Float>()
    val totalDistance: LiveData<Float> = _totalDistance

    private val _duration = MutableLiveData<Long>()
    val duration: LiveData<Long> = _duration

    private val _trajectoryPoints = MutableLiveData<List<TrajectoryPoint>>()
    val trajectoryPoints: LiveData<List<TrajectoryPoint>> = _trajectoryPoints

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _sensorStatus = MutableLiveData<Map<String, Boolean>>()
    val sensorStatus: LiveData<Map<String, Boolean>> = _sensorStatus

    private val _savedTrajectories = MutableLiveData<List<Trajectory>>()
    val savedTrajectories: LiveData<List<Trajectory>> = _savedTrajectories

    private var currentTrajectoryId: Long = 0

    init {
        _isRecording.value = false
        initRepositories()
        checkSensors()
        loadSavedTrajectories()
    }

    private fun initRepositories() {
        val context = getApplication<Application>()
        val sensorController = SensorController(context)
        pdrRepository = PDRRepository(sensorController)

        val database = TrajectoryDatabase.getInstance(context)
        val dao = database.trajectoryDao()
        trajectoryRepository = TrajectoryRepository(dao)

        serviceManager = PDRServiceManager(context)
    }

    private fun checkSensors() {
        _sensorStatus.value = pdrRepository.getSensorAvailability()
        if (!pdrRepository.hasRequiredSensors()) {
            _statusMessage.value = "设备缺少必要传感器"
        }
    }

    private fun loadSavedTrajectories() {
        viewModelScope.launch {
            trajectoryRepository.getAllTrajectories()
                .onEach { trajectories ->
                    _savedTrajectories.value = trajectories
                }
                .catch { e ->
                    _statusMessage.value = "加载历史轨迹失败: ${e.message}"
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * 开始记录轨迹
     */
    fun startRecording() {
        if (_isRecording.value == true) return

        // 重置UI状态
        _trajectoryPoints.value = emptyList()
        _totalSteps.value = 0
        _totalDistance.value = 0f
        _duration.value = 0L
        _statusMessage.value = "正在启动..."

        // 重置服务中的轨迹数据
        serviceManager.resetPosition()

        // 先设置监听器
        serviceManager.setOnPositionUpdateListener { point ->
            val currentPoints = _trajectoryPoints.value?.toMutableList() ?: mutableListOf()
            currentPoints.add(point)
            _trajectoryPoints.postValue(currentPoints)
            _totalSteps.postValue(serviceManager.getTotalSteps())
            _totalDistance.postValue(serviceManager.getTotalDistance())
            _duration.postValue(serviceManager.getDuration())
            _currentPosition.postValue(serviceManager.getCurrentPosition())
            _currentHeading.postValue(serviceManager.getCurrentHeading())
        }

        // 启动前台服务，等待绑定完成
        serviceManager.startAndBind(currentTrajectoryId) {
            // 服务绑定完成后的回调
            _isRecording.postValue(true)
            _statusMessage.postValue("开始记录轨迹")
        }
    }

    /**
     * 停止记录轨迹
     */
    fun stopRecording() {
        if (_isRecording.value != true) return

        serviceManager.pause()

        // 保存轨迹
        saveCurrentTrajectory()

        _isRecording.value = false
        _statusMessage.value = "轨迹已保存"
    }

    /**
     * 保存当前轨迹到数据库
     */
    private fun saveCurrentTrajectory() {
        viewModelScope.launch {
            val points = serviceManager.getTrajectoryPoints()
            if (points.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            val trajectory = Trajectory(
                name = "轨迹_${dateFormat.format(Date(now))}",
                startTime = now,
                endTime = now,
                totalSteps = _totalSteps.value ?: 0,
                totalDistance = _totalDistance.value ?: 0f
            )

            try {
                trajectoryRepository.saveTrajectory(trajectory, points)
                _statusMessage.value = "轨迹已保存"
            } catch (e: Exception) {
                _statusMessage.value = "保存失败: ${e.message}"
            }
        }
    }

    /**
     * 校准航向
     */
    fun calibrateHeading() {
        serviceManager.calibrateHeading()
        _statusMessage.value = "航向已校准"
    }

    /**
     * 清除当前轨迹
     */
    fun clearTrajectory() {
        serviceManager.resetPosition()
        _trajectoryPoints.value = emptyList()
        _totalSteps.value = 0
        _totalDistance.value = 0f
        _duration.value = 0L
        _currentPosition.value = Pair(0f, 0f)
        _statusMessage.value = "轨迹已清除"
    }

    /**
     * 加载历史轨迹
     */
    fun loadTrajectory(trajectoryId: Long) {
        viewModelScope.launch {
            try {
                val points = trajectoryRepository.getTrajectoryPointsOnce(trajectoryId)
                _trajectoryPoints.value = points

                val trajectory = trajectoryRepository.getTrajectoryById(trajectoryId)
                trajectory?.let {
                    _totalSteps.value = it.totalSteps
                    _totalDistance.value = it.totalDistance
                }
                _statusMessage.value = "已加载历史轨迹"
            } catch (e: Exception) {
                _statusMessage.value = "加载失败: ${e.message}"
            }
        }
    }

    /**
     * 删除历史轨迹
     */
    fun deleteTrajectory(trajectoryId: Long) {
        viewModelScope.launch {
            try {
                trajectoryRepository.deleteTrajectory(trajectoryId)
                _statusMessage.value = "轨迹已删除"
            } catch (e: Exception) {
                _statusMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 格式化时长
     */
    fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceManager.unbind()
    }
}