package com.example.habittracker.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.habittracker.data.local.room.entity.AchievementProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievement_progress ORDER BY category, key")
    fun observeAll(): Flow<List<AchievementProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AchievementProgressEntity>)

    @Query("UPDATE achievement_progress SET target = :target, updatedAtMillis = :updatedAtMillis WHERE key = :key")
    suspend fun updateTarget(key: String, target: Int, updatedAtMillis: Long)
}
