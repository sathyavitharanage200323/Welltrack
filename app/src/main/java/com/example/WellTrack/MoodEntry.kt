package com.example.WellTrack

import java.util.UUID

data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String,
    val moodName: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

//emoji data model
object MoodEmojis {
    val moods = listOf(
        MoodOption("😊", "Happy"),
        MoodOption("😔", "Sad"),
        MoodOption("😰", "Anxious"),
        MoodOption("😌", "Calm"),
        MoodOption("😡", "Angry"),
        MoodOption("😴", "Tired"),
        MoodOption("🤗", "Grateful"),
        MoodOption("😎", "Confident"),
        MoodOption("😢", "Crying"),
        MoodOption("🥳", "Excited"),
        MoodOption("😐", "Neutral"),
        MoodOption("🤔", "Thoughtful")
    )
}

data class MoodOption(
    val emoji: String,
    val name: String
)
