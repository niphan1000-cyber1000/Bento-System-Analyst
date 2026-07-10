package com.aistudio.aisystemanalyst.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val email: String,
    val passwordHash: String,
    val salt: String = "",
    val role: String = "System Analyst",
    val createdAt: Long = System.currentTimeMillis()
)
