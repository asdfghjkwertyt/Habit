package com.example.habittracker.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.habittracker.data.local.room.dao.AchievementDao
import com.example.habittracker.data.local.room.dao.HabitDao
import com.example.habittracker.data.local.room.dao.HabitLogDao
import com.example.habittracker.data.local.room.entity.AchievementProgressEntity
import com.example.habittracker.data.local.room.entity.Habit
import com.example.habittracker.data.local.room.entity.HabitLog

@Database(
    entities = [Habit::class, HabitLog::class, AchievementProgressEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(HabitTypeConverters::class)
abstract class HabitTrackerDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun achievementDao(): AchievementDao
}
