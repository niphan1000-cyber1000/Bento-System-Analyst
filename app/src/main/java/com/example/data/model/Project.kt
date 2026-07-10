package com.aistudio.aisystemanalyst.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val industry: String,
    val description: String,
    val rawContent: String = "",
    val requirementsReport: String? = null,
    val businessReport: String? = null,
    val systemReport: String? = null,
    val databaseReport: String? = null,
    val apiReport: String? = null,
    val securityReport: String? = null,
    val performanceReport: String? = null,
    val codeReport: String? = null,
    val infraReport: String? = null,
    val riskReport: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
