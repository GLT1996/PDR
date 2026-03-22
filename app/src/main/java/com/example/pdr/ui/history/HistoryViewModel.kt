package com.example.pdr.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdr.data.local.TrajectoryDatabase
import com.example.pdr.data.model.Trajectory
import com.example.pdr.data.repository.TrajectoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrajectoryRepository

    private val _trajectories = MutableLiveData<List<Trajectory>>()
    val trajectories: LiveData<List<Trajectory>> = _trajectories

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    init {
        val dao = TrajectoryDatabase.getInstance(application).trajectoryDao()
        repository = TrajectoryRepository(dao)
        loadTrajectories()
    }

    fun loadTrajectories() {
        viewModelScope.launch {
            repository.getAllTrajectories().collect { list ->
                _trajectories.postValue(list)
            }
        }
    }

    fun deleteTrajectories(ids: List<Long>) {
        viewModelScope.launch {
            try {
                ids.forEach { id ->
                    repository.deleteTrajectory(id)
                }
                _statusMessage.value = "已删除 ${ids.size} 条轨迹"
            } catch (e: Exception) {
                _statusMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    fun deleteTrajectory(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTrajectory(id)
                _statusMessage.value = "轨迹已删除"
            } catch (e: Exception) {
                _statusMessage.value = "删除失败: ${e.message}"
            }
        }
    }
}