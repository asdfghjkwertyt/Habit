package com.example.habittracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun formatEpochDay(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(dateFormatter)

    fun lastNDays(n: Int): List<Long> {
        val today = todayEpochDay()
        return (0 until n).map { today - it }.reversed()
    }

    fun longestStreak(days: List<Long>): Int {
        if (days.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] + 1) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }

    fun currentStreak(days: List<Long>, todayEpochDay: Long): Int {
        if (days.isEmpty()) return 0
        val daySet = days.toSet()
        var day = todayEpochDay
        var streak = 0
        while (daySet.contains(day)) {
            streak++
            day--
        }
        return streak
    }
}
