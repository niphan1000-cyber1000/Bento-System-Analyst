package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_items")
data class RiskItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String,
    val severity: String, // "Low", "Medium", "High"
    val category: String, // "Technical", "Business", "Security", "Infrastructure"
    val status: String, // "Identified", "Mitigating", "Resolved"
    val mitigationPlan: String,
    val createdAt: Long = System.currentTimeMillis()
)
