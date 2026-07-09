package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_tasks")
data class ProjectTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String,
    val description: String,
    val status: String, // "Todo", "In Progress", "Review", "Done"
    val priority: String, // "Low", "Medium", "High"
    val assignee: String,
    val sprint: String = "Sprint 1",
    val createdAt: Long = System.currentTimeMillis()
)
