package com.example.pdr.data.repository

import com.example.pdr.data.local.TrajectoryDao
import com.example.pdr.data.model.Trajectory
import com.example.pdr.data.model.TrajectoryPoint
import kotlinx.coroutines.flow.Flow

/**
 * 轨迹存储仓库
 * 负责轨迹数据的持久化存储和读取
 */
class TrajectoryRepository(private val trajectoryDao: TrajectoryDao) {

    /**
     * 保存轨迹
     * @param trajectory 轨迹信息
     * @param points 轨迹点列表
     * @return 保存的轨迹ID
     */
    suspend fun saveTrajectory(
        trajectory: Trajectory,
        points: List<TrajectoryPoint>
    ): Long {
        return trajectoryDao.saveTrajectoryWithPoints(trajectory, points)
    }

    /**
     * 更新轨迹信息
     */
    suspend fun updateTrajectory(trajectory: Trajectory) {
        trajectoryDao.updateTrajectory(trajectory)
    }

    /**
     * 删除轨迹
     */
    suspend fun deleteTrajectory(trajectoryId: Long) {
        trajectoryDao.deleteTrajectoryWithPoints(trajectoryId)
    }

    /**
     * 获取所有轨迹列表
     */
    fun getAllTrajectories(): Flow<List<Trajectory>> {
        return trajectoryDao.getAllTrajectories()
    }

    /**
     * 获取最近的轨迹
     */
    fun getRecentTrajectories(limit: Int = 10): Flow<List<Trajectory>> {
        return trajectoryDao.getRecentTrajectories(limit)
    }

    /**
     * 根据ID获取轨迹
     */
    suspend fun getTrajectoryById(id: Long): Trajectory? {
        return trajectoryDao.getTrajectoryById(id)
    }

    /**
     * 获取轨迹的所有轨迹点
     */
    fun getTrajectoryPoints(trajectoryId: Long): Flow<List<TrajectoryPoint>> {
        return trajectoryDao.getPointsForTrajectory(trajectoryId)
    }

    /**
     * 一次性获取轨迹点
     */
    suspend fun getTrajectoryPointsOnce(trajectoryId: Long): List<TrajectoryPoint> {
        return trajectoryDao.getPointsForTrajectoryOnce(trajectoryId)
    }

    /**
     * 获取轨迹点数量
     */
    suspend fun getPointCount(trajectoryId: Long): Int {
        return trajectoryDao.getPointCount(trajectoryId)
    }
}