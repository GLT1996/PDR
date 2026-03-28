package com.example.pdr.ui.location

import android.app.Application
import android.location.Location
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

class LocationViewModel(application: Application) : AndroidViewModel(application) {

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

    init {
        _isLocating.value = false
        _locationState.value = LocationState.IDLE
        _statusMessage.value = "未开始定位"
        _updateCountText.value = "更新次数: 0"
        _diagnosticInfo.value = "点击开始定位获取位置信息"
        initLocationCallback()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationData(location)
                } ?: run {
                    // 收到回调但没有位置数据
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
            setWaitForAccurateLocation(false)  // 不等待高精度，先返回任何位置
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                getApplication<Application>().mainLooper
            )
            _isLocating.value = true

            // 先尝试获取最后已知位置
            getLastLocation()

        } catch (e: SecurityException) {
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
        _isLocating.value = false
        _locationState.value = LocationState.IDLE
        _statusMessage.value = "定位已停止"
        _diagnosticInfo.value = "定位服务已停止。共获取 ${updateCount} 次位置更新"
    }

    /**
     * 更新位置数据显示
     */
    private fun updateLocationData(location: Location) {
        updateCount++
        _updateCountText.postValue("更新次数: $updateCount")

        _longitude.postValue(String.format("%.6f", location.longitude))
        _latitude.postValue(String.format("%.6f", location.latitude))
        _accuracy.postValue(String.format("%.1f 米", location.accuracy))
        _speed.postValue(String.format("%.1f m/s", location.speed))
        _bearing.postValue(String.format("%.1f°", location.bearing))

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
    }
}