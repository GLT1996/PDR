package com.example.pdr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 轨迹实体类
 * 表示一次完整的PDR记录轨迹
 */
@Entity(tableName = "trajectories")
data class Trajectory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalSteps: Int = 0,
    val totalDistance: Float = 0f,
    val startPointName: String? = null,
    val endPointName: String? = null
)