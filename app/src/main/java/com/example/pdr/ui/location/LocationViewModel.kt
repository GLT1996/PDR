package com.example.pdr.ui.location

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    companion object {
        private const val TAG = "LocationViewModel"
    }

    // 传感器管理
    private val sensorManager: SensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // 传感器数据缓存
    private var accelerometerValues: FloatArray? = null
    private var magnetometerValues: FloatArray? = null

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private var locationCallback: LocationCallback? = null

    // 定位状态枚举
    enum class LocationState {
        IDLE,               // 未开始
        SEARCHING_GPS,      // 正在搜索GPS信号
        SEARCHING_NETWORK,  // 正在搜索网络定位
        LOCATED,            // 已定位
        NO_SIGNAL,          // 无信号
        PERMISSION_DENIED   // 权限被拒绝
    }

    // 状态
    private val _isLocating = MutableLiveData<Boolean>()
    val isLocating: LiveData<Boolean> = _isLocating

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _diagnosticInfo = MutableLiveData<String>()
    val diagnosticInfo: LiveData<String> = _diagnosticInfo

    // 更新计数
    private var updateCount = 0
    private val _updateCountText = MutableLiveData<String>()
    val updateCountText: LiveData<String> = _updateCountText

    // 位置数据
    private val _longitude = MutableLiveData<String>()
    val longitude: LiveData<String> = _longitude

    private val _latitude = MutableLiveData<String>()
    val latitude: LiveData<String> = _latitude

    private val _accuracy = MutableLiveData<String>()
    val accuracy: LiveData<String> = _accuracy

    private val _speed = MutableLiveData<String>()
    val speed: LiveData<String> = _speed

    private val _bearing = MutableLiveData<String>()
    val bearing: LiveData<String> = _bearing

    private val _provider = MutableLiveData<String>()
    val provider: LiveData<String> = _provider

    private val _updateTime = MutableLiveData<String>()
    val updateTime: LiveData<String> = _updateTime

    // 设备方向（来自传感器）
    private val _deviceAzimuth = MutableLiveData<Float>()
    val deviceAzimuth: LiveData<Float> = _deviceAzimuth

    private val _deviceBearing = MutableLiveData<String>()
    val deviceBearing: LiveData<String> = _deviceBearing

    // 罗盘是否启用
    private val _compassEnabled = MutableLiveData<Boolean>()
    val compassEnabled: LiveData<Boolean> = _compassEnabled

    init {
        _isLocating.value = false
        _locationState.value = LocationState.IDLE
        _statusMessage.value = "未开始定位"
        _updateCountText.value = "更新次数: 0"
        _diagnosticInfo.value = "点击开始定位获取位置信息"
        _deviceAzimuth.value = 0f
        _deviceBearing.value = "--°"
        _compassEnabled.value = false
        initLocationCallback()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(TAG, "onLocationResult called, locations count: ${locationResult.locations.size}")
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Location update: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}, provider=${location.provider}")
                    updateLocationData(location)
                } ?: run {
                    // 收到回调但没有位置数据
                    Log.w(TAG, "onLocationResult called but lastLocation is null")
                    _locationState.postValue(LocationState.NO_SIGNAL)
                    _statusMessage.postValue("收到定位回调但无位置数据")
                    updateDiagnosticInfo("定位服务响应了，但未能获取位置。可能原因：室内GPS信号弱、定位服务未完全启用")
                }
            }
        }
    }

    /**
     * 开始实时位置更新
     */
    fun startLocationUpdates() {
        if (_isLocating.value == true) return

        updateCount = 0
        _updateCountText.value = "更新次数: 0"
        _locationState.value = LocationState.SEARCHING_GPS
        _statusMessage.value = "正在启动定位服务..."
        _diagnosticInfo.value = "请求GPS定位中...如果长时间无更新，请检查：1.系统定位开关是否开启 2.是否授予精确位置权限 3.是否在室内(GPS信号弱)"

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L  // 更新间隔：1秒
        ).apply {
            setMinUpdateIntervalMillis(500L)  // 最快更新间隔：0.5秒
            setWaitForAccurateLocation(true)   // 等待高精度位置，确保持续更新
            setMinUpdateDistanceMeters(0f)     // 距离变化为0也更新（确保移动时更新）
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                getApplication<Application>().mainLooper
            ).addOnSuccessListener {
                Log.d(TAG, "Location updates requested successfully")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to request location updates", e)
                _locationState.postValue(LocationState.NO_SIGNAL)
                _statusMessage.postValue("启动定位失败: ${e.message}")
            }
            _isLocating.value = true

            // 启用方向传感器
            startCompass()

            // 先尝试获取最后已知位置
            getLastLocation()

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: missing location permission", e)
            _locationState.value = LocationState.PERMISSION_DENIED
            _statusMessage.value = "缺少定位权限"
            _diagnosticInfo.value = "权限被拒绝。请在系统设置中授予位置权限"
        }
    }

    /**
     * 停止位置更新
     */
    fun stopLocationUpdates() {
        if (_isLocating.value != true) return

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        stopCompass()
        _isLocating.value = false
        _locationState.value = LocationState.IDLE
        _statusMessage.value = "定位已停止"
        _diagnosticInfo.value = "定位服务已停止。共获取 ${updateCount} 次位置更新"
    }

    /**
     * 更新位置数据显示
     */
    private fun updateLocationData(location: Location) {
        Log.d(TAG, "updateLocationData: count=${updateCount + 1}, lat=${location.latitude}, lon=${location.longitude}")
        updateCount++
        _updateCountText.postValue("更新次数: $updateCount")

        _longitude.postValue(String.format("%.6f", location.longitude))
        _latitude.postValue(String.format("%.6f", location.latitude))
        _accuracy.postValue(String.format("%.1f 米", location.accuracy))
        _speed.postValue(String.format("%.1f m/s", location.speed))
        // GPS bearing显示移动方向，仅在移动时有效
        _bearing.postValue(String.format("%.1f° (GPS)", location.bearing))

        val providerName = when (location.provider) {
            "fused" -> "融合定位"
            "gps" -> "GPS"
            "network" -> "网络定位"
            else -> location.provider ?: "未知"
        }
        _provider.postValue(providerName)

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        _updateTime.postValue(timeFormat.format(Date(location.time)))

        // 根据精度判断定位状态
        if (location.accuracy <= 20) {
            _locationState.postValue(LocationState.LOCATED)
            _statusMessage.postValue("GPS定位成功 (高精度)")
            updateDiagnosticInfo("GPS信号良好，精度 ${String.format("%.1f", location.accuracy)} 米")
        } else if (location.accuracy <= 100) {
            _locationState.postValue(LocationState.LOCATED)
            _statusMessage.postValue("已定位 (中等精度)")
            updateDiagnosticInfo("定位成功，精度 ${String.format("%.1f", location.accuracy)} 米。可能是网络定位或GPS信号较弱")
        } else {
            _locationState.postValue(LocationState.SEARCHING_NETWORK)
            _statusMessage.postValue("已定位 (低精度)")
            updateDiagnosticInfo("精度较低 (${String.format("%.1f", location.accuracy)} 米)。建议：到户外获取GPS信号，或检查是否授予精确位置权限")
        }
    }

    private fun updateDiagnosticInfo(info: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _diagnosticInfo.postValue("[$timestamp] $info")
    }

    /**
     * 获取最后已知位置
     */
    fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateLocationData(it)
                    _statusMessage.postValue("已获取最后已知位置")
                } ?: run {
                    _statusMessage.postValue("无最后已知位置，等待新定位...")
                    updateDiagnosticInfo("设备没有缓存的位置数据，正在等待定位服务获取新位置")
                }
            }.addOnFailureListener { e ->
                _statusMessage.postValue("获取位置失败: ${e.message}")
                updateDiagnosticInfo("定位服务异常: ${e.message}")
            }
        } catch (e: SecurityException) {
            _locationState.value = LocationState.PERMISSION_DENIED
            _statusMessage.value = "缺少定位权限"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        stopCompass()
    }

    // ========== 方向传感器相关 ==========

    /**
     * 启动罗盘（方向传感器）
     */
    private fun startCompass() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        _compassEnabled.value = true
        Log.d(TAG, "Compass started")
    }

    /**
     * 停止罗盘
     */
    private fun stopCompass() {
        sensorManager.unregisterListener(this)
        _compassEnabled.value = false
        accelerometerValues = null
        magnetometerValues = null
        Log.d(TAG, "Compass stopped")
    }

    /**
     * SensorEventListener 实现
     */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerValues = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerValues = event.values.clone()
            }
        }

        // 当两个传感器数据都有时，计算方向
        if (accelerometerValues != null && magnetometerValues != null) {
            calculateAzimuth()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 可以根据精度调整行为
        Log.d(TAG, "Sensor ${sensor.name} accuracy changed to $accuracy")
    }

    /**
     * 计算方位角
     */
    private fun calculateAzimuth() {
        val acc = accelerometerValues ?: return
        val mag = magnetometerValues ?: return

        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)

        // 获取旋转矩阵
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, acc, mag)

        if (success) {
            // 获取方向值（azimuth, pitch, roll）
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            // azimuth 范围是 -π 到 π，转换为 0-360度
            var azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            azimuth = (azimuth + 360) % 360 // 转换为 0-360

            // 低通滤波平滑处理
            val previousAzimuth = _deviceAzimuth.value ?: 0f
            val smoothedAzimuth = smoothAzimuth(previousAzimuth, azimuth)

            _deviceAzimuth.postValue(smoothedAzimuth)
            _deviceBearing.postValue(String.format("%.1f° (%s)", smoothedAzimuth, getDirectionName(smoothedAzimuth)))

            Log.d(TAG, "Azimuth: $smoothedAzimuth°")
        }
    }

    /**
     * 平滑方位角变化（低通滤波）
     */
    private fun smoothAzimuth(previous: Float, current: Float): Float {
        val alpha = 0.3f // 平滑系数

        // 处理跨越0/360边界的情况
        var diff = current - previous
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        return previous + alpha * diff
    }

    /**
     * 根据方位角返回方向名称
     */
    private fun getDirectionName(azimuth: Float): String {
        val normalizedAzimuth = ((azimuth % 360) + 360) % 360

        return when {
            normalizedAzimuth < 22.5 || normalizedAzimuth >= 337.5 -> "北"
            normalizedAzimuth < 67.5 -> "东北"
            normalizedAzimuth < 112.5 -> "东"
            normalizedAzimuth < 157.5 -> "东南"
            normalizedAzimuth < 202.5 -> "南"
            normalizedAzimuth < 247.5 -> "西南"
            normalizedAzimuth < 292.5 -> "西"
            else -> "西北"
        }
    }
}