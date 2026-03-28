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

    // 状态
    private val _isLocating = MutableLiveData<Boolean>()
    val isLocating: LiveData<Boolean> = _isLocating

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

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
        _statusMessage.value = "未开始定位"
        initLocationCallback()
    }

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationData(location)
                }
            }
        }
    }

    /**
     * 开始实时位置更新
     */
    fun startLocationUpdates() {
        if (_isLocating.value == true) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L  // 更新间隔：1秒
        ).apply {
            setMinUpdateIntervalMillis(500L)  // 最快更新间隔：0.5秒
            setWaitForAccurateLocation(true)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                getApplication<Application>().mainLooper
            )
            _isLocating.value = true
            _statusMessage.value = "正在定位..."
        } catch (e: SecurityException) {
            _statusMessage.value = "缺少定位权限"
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
        _statusMessage.value = "定位已停止"
    }

    /**
     * 更新位置数据显示
     */
    private fun updateLocationData(location: Location) {
        _longitude.value = String.format("%.6f", location.longitude)
        _latitude.value = String.format("%.6f", location.latitude)
        _accuracy.value = String.format("%.1f 米", location.accuracy)
        _speed.value = String.format("%.1f m/s", location.speed)
        _bearing.value = String.format("%.1f°", location.bearing)
        _provider.value = location.provider ?: "未知"

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        _updateTime.value = timeFormat.format(Date(location.time))

        _statusMessage.value = "位置已更新"
    }

    /**
     * 获取最后已知位置
     */
    fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateLocationData(it)
                    _statusMessage.value = "已获取最后位置"
                } ?: run {
                    _statusMessage.value = "无已知位置，请开始定位"
                }
            }.addOnFailureListener {
                _statusMessage.value = "获取位置失败"
            }
        } catch (e: SecurityException) {
            _statusMessage.value = "缺少定位权限"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}