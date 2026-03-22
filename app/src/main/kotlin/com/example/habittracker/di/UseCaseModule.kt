package com.example.habittracker.di

import com.example.habittracker.domain.usecase.AddHabitUseCase
import com.example.habittracker.domain.usecase.DeleteHabitUseCase
import com.example.habittracker.domain.usecase.GetAllCompletionsUseCase
import com.example.habittracker.domain.usecase.GetHabitByIdUseCase
import com.example.habittracker.domain.usecase.GetHabitCompletionsUseCase
import com.example.habittracker.domain.usecase.GetHabitStreakUseCase
import com.example.habittracker.domain.usecase.GetHabitStatsUseCase
import com.example.habittracker.domain.usecase.GetHabitsUseCase
import com.example.habittracker.domain.usecase.GetAchievementProgressUseCase
import com.example.habittracker.domain.usecase.HabitUseCases
import com.example.habittracker.domain.usecase.MarkHabitCompleteUseCase
import com.example.habittracker.domain.usecase.SetHabitNoteUseCase
import com.example.habittracker.domain.usecase.SyncAchievementProgressUseCase
import com.example.habittracker.domain.usecase.UpdateAchievementTargetUseCase
import com.example.habittracker.domain.usecase.UpdateHabitUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideHabitUseCases(
        getHabits: GetHabitsUseCase,
        addHabit: AddHabitUseCase,
        updateHabit: UpdateHabitUseCase,
        deleteHabit: DeleteHabitUseCase,
        markHabitComplete: MarkHabitCompleteUseCase,
        setHabitNote: SetHabitNoteUseCase,
        getHabitStreak: GetHabitStreakUseCase,
        getHabitById: GetHabitByIdUseCase,
        getAllCompletions: GetAllCompletionsUseCase,
        getHabitCompletions: GetHabitCompletionsUseCase,
        getHabitStats: GetHabitStatsUseCase,
        getAchievementProgress: GetAchievementProgressUseCase,
        syncAchievementProgress: SyncAchievementProgressUseCase,
        updateAchievementTarget: UpdateAchievementTargetUseCase
    ): HabitUseCases = HabitUseCases(
        getHabits = getHabits,
        addHabit = addHabit,
        updateHabit = updateHabit,
        deleteHabit = deleteHabit,
        markHabitComplete = markHabitComplete,
        setHabitNote = setHabitNote,
        getHabitStreak = getHabitStreak,
        getHabitById = getHabitById,
        getAllCompletions = getAllCompletions,
        getHabitCompletions = getHabitCompletions,
        getHabitStats = getHabitStats,
        getAchievementProgress = getAchievementProgress,
        syncAchievementProgress = syncAchievementProgress,
        updateAchievementTarget = updateAchievementTarget
    )
}
