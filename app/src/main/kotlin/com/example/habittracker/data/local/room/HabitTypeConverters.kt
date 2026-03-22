package com.example.habittracker.data.local.room

import androidx.room.TypeConverter
import com.example.habittracker.data.local.room.entity.HabitFrequency

class HabitTypeConverters {

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)

    @TypeConverter
    fun fromHabitFrequency(value: HabitFrequency): String = value.name
}
