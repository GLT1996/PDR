package com.example.pdr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pdr.data.model.Trajectory
import com.example.pdr.data.model.TrajectoryPoint

/**
 * Room数据库配置
 * 存储PDR轨迹数据
 */
@Database(
    entities = [Trajectory::class, TrajectoryPoint::class],
    version = 1,
    exportSchema = false
)
abstract class TrajectoryDatabase : RoomDatabase() {

    abstract fun trajectoryDao(): TrajectoryDao

    companion object {
        private const val DATABASE_NAME = "pdr_trajectory.db"

        @Volatile
        private var INSTANCE: TrajectoryDatabase? = null

        fun getInstance(context: Context): TrajectoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): TrajectoryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TrajectoryDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}