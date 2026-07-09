package com.example.data.local

import androidx.room.*
import com.example.data.model.AuditLog
import com.example.data.model.Project
import com.example.data.model.ProjectTask
import com.example.data.model.RiskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksForProject(projectId: Int): Flow<List<ProjectTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTask)

    @Update
    suspend fun updateTask(task: ProjectTask)

    @Delete
    suspend fun deleteTask(task: ProjectTask)

    @Query("SELECT * FROM risk_items WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getRisksForProject(projectId: Int): Flow<List<RiskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRisk(risk: RiskItem)

    @Update
    suspend fun updateRisk(risk: RiskItem)

    @Delete
    suspend fun deleteRisk(risk: RiskItem)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): com.example.data.model.User?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: com.example.data.model.User): Long
}
