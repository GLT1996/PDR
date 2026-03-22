package com.example.pdr.ui.history

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdr.data.local.TrajectoryDatabase
import com.example.pdr.data.model.Trajectory
import com.example.pdr.data.repository.TrajectoryRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrajectoryRepository

    private val _trajectories = MutableLiveData<List<Trajectory>>()
    val trajectories: LiveData<List<Trajectory>> = _trajectories

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    init {
        Log.d("HistoryViewModel", "init: initializing")
        val database = TrajectoryDatabase.getInstance(application)
        val dao = database.trajectoryDao()
        repository = TrajectoryRepository(dao)

        // 先执行一次性查询测试
        viewModelScope.launch {
            try {
                val testList = dao.getAllTrajectoriesOnce()
                Log.d("HistoryViewModel", "init: one-time query found ${testList.size} trajectories")
                testList.forEach {
                    Log.d("HistoryViewModel", "  - trajectory: id=${it.id}, name=${it.name}")
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "init: one-time query failed: ${e.message}", e)
            }
        }

        loadTrajectories()
    }

    fun loadTrajectories() {
        Log.d("HistoryViewModel", "loadTrajectories: starting to observe")
        viewModelScope.launch {
            repository.getAllTrajectories()
                .onEach { list ->
                    Log.d("HistoryViewModel", "loadTrajectories: received ${list.size} trajectories")
                    list.forEach {
                        Log.d("HistoryViewModel", "  - trajectory: id=${it.id}, name=${it.name}")
                    }
                    _trajectories.postValue(list)
                }
                .catch { e ->
                    Log.e("HistoryViewModel", "loadTrajectories error: ${e.message}", e)
                    _statusMessage.postValue("加载失败: ${e.message}")
                }
                .launchIn(viewModelScope)
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