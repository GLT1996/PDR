package com.example.pdr.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 轨迹点实体类
 * 表示轨迹中的一个位置点
 */
@Entity(
    tableName = "trajectory_points",
    foreignKeys = [
        ForeignKey(
            entity = Trajectory::class,
            parentColumns = ["id"],
            childColumns = ["trajectoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trajectoryId"])]
)
data class TrajectoryPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trajectoryId: Long,
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val heading: Float,
    val stepCount: Int = 0
)