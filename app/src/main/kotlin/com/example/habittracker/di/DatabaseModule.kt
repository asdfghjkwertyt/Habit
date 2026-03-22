package com.example.habittracker.di

import android.content.Context
import androidx.room.Room
import com.example.habittracker.data.local.room.HabitTrackerDatabase
import com.example.habittracker.data.local.room.dao.AchievementDao
import com.example.habittracker.data.local.room.dao.HabitDao
import com.example.habittracker.data.local.room.dao.HabitLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HabitTrackerDatabase {
        return Room.databaseBuilder(
            context,
            HabitTrackerDatabase::class.java,
            "habit_tracker.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideHabitDao(database: HabitTrackerDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideHabitLogDao(database: HabitTrackerDatabase): HabitLogDao = database.habitLogDao()

    @Provides
    fun provideAchievementDao(database: HabitTrackerDatabase): AchievementDao = database.achievementDao()
}
