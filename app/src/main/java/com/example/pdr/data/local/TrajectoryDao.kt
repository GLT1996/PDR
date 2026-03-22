package com.example.pdr.data.local

import androidx.room.*
import com.example.pdr.data.model.Trajectory
import com.example.pdr.data.model.TrajectoryPoint
import kotlinx.coroutines.flow.Flow

/**
 * 轨迹数据访问对象
 * 提供轨迹和轨迹点的CRUD操作
 */
@Dao
interface TrajectoryDao {

    // ============ Trajectory 操作 ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrajectory(trajectory: Trajectory): Long

    @Update
    suspend fun updateTrajectory(trajectory: Trajectory)

    @Delete
    suspend fun deleteTrajectory(trajectory: Trajectory)

    @Query("DELETE FROM trajectories WHERE id = :trajectoryId")
    suspend fun deleteTrajectoryById(trajectoryId: Long)

    @Query("SELECT * FROM trajectories WHERE id = :id")
    suspend fun getTrajectoryById(id: Long): Trajectory?

    @Query("SELECT * FROM trajectories ORDER BY startTime DESC")
    fun getAllTrajectories(): Flow<List<Trajectory>>

    @Query("SELECT * FROM trajectories ORDER BY startTime DESC")
    suspend fun getAllTrajectoriesOnce(): List<Trajectory>

    @Query("SELECT * FROM trajectories ORDER BY startTime DESC LIMIT :limit")
    fun getRecentTrajectories(limit: Int): Flow<List<Trajectory>>

    // ============ TrajectoryPoint 操作 ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrajectoryPoint(point: TrajectoryPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrajectoryPoints(points: List<TrajectoryPoint>)

    @Query("SELECT * FROM trajectory_points WHERE trajectoryId = :trajectoryId ORDER BY timestamp ASC")
    fun getPointsForTrajectory(trajectoryId: Long): Flow<List<TrajectoryPoint>>

    @Query("SELECT * FROM trajectory_points WHERE trajectoryId = :trajectoryId ORDER BY timestamp ASC")
    suspend fun getPointsForTrajectoryOnce(trajectoryId: Long): List<TrajectoryPoint>

    @Query("DELETE FROM trajectory_points WHERE trajectoryId = :trajectoryId")
    suspend fun deletePointsForTrajectory(trajectoryId: Long)

    @Query("SELECT COUNT(*) FROM trajectory_points WHERE trajectoryId = :trajectoryId")
    suspend fun getPointCount(trajectoryId: Long): Int

    // ============ 批量操作 ============

    @Transaction
    suspend fun deleteTrajectoryWithPoints(trajectoryId: Long) {
        deletePointsForTrajectory(trajectoryId)
        deleteTrajectoryById(trajectoryId)
    }

    @Transaction
    suspend fun saveTrajectoryWithPoints(
        trajectory: Trajectory,
        points: List<TrajectoryPoint>
    ): Long {
        val trajectoryId = insertTrajectory(trajectory)
        val pointsWithId = points.map { it.copy(trajectoryId = trajectoryId) }
        insertTrajectoryPoints(pointsWithId)
        return trajectoryId
    }
}