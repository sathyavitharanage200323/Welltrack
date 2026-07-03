package com.example.WellTrack

import java.util.UUID

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    var name: String
)

data class DailyCompletion(
    val habitId: String,
    val date: String, // Format: YYYY-MM-DD
    var isCompleted: Boolean = false
)