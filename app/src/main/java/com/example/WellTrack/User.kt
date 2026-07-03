package com.example.WellTrack

data class User(
    val email: String,
    val name: String,
    val password: String, // In production, this should be hashed
    val createdAt: Long = System.currentTimeMillis()
)
