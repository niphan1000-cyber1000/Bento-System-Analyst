package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // e.g. "Login", "Upload", "Delete", "AI Analysis", "Export", "Permission Change"
    val userRole: String, // e.g. "System Analyst", "Admin", etc.
    val details: String
)
