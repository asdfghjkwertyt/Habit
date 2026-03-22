package com.example.habittracker.domain.usecase

data class HabitUseCases(
    val getHabits: GetHabitsUseCase,
    val addHabit: AddHabitUseCase,
    val updateHabit: UpdateHabitUseCase,
    val deleteHabit: DeleteHabitUseCase,
    val markHabitComplete: MarkHabitCompleteUseCase,
    val setHabitNote: SetHabitNoteUseCase,
    val getHabitStreak: GetHabitStreakUseCase,
    val getHabitById: GetHabitByIdUseCase,
    val getAllCompletions: GetAllCompletionsUseCase,
    val getHabitCompletions: GetHabitCompletionsUseCase,
    val getHabitStats: GetHabitStatsUseCase,
    val getAchievementProgress: GetAchievementProgressUseCase,
    val syncAchievementProgress: SyncAchievementProgressUseCase,
    val updateAchievementTarget: UpdateAchievementTargetUseCase
)
