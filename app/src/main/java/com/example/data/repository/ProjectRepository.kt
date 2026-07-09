package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.model.AuditLog
import com.example.data.model.Project
import com.example.data.model.ProjectTask
import com.example.data.model.RiskItem
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allAuditLogs: Flow<List<AuditLog>> = projectDao.getAllAuditLogs()

    suspend fun getProjectById(id: Int): Project? {
        return projectDao.getProjectById(id)
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project)
    }

    fun getTasksForProject(projectId: Int): Flow<List<ProjectTask>> {
        return projectDao.getTasksForProject(projectId)
    }

    suspend fun insertTask(task: ProjectTask) {
        projectDao.insertTask(task)
    }

    suspend fun updateTask(task: ProjectTask) {
        projectDao.updateTask(task)
    }

    suspend fun deleteTask(task: ProjectTask) {
        projectDao.deleteTask(task)
    }

    fun getRisksForProject(projectId: Int): Flow<List<RiskItem>> {
        return projectDao.getRisksForProject(projectId)
    }

    suspend fun insertRisk(risk: RiskItem) {
        projectDao.insertRisk(risk)
    }

    suspend fun updateRisk(risk: RiskItem) {
        projectDao.updateRisk(risk)
    }

    suspend fun deleteRisk(risk: RiskItem) {
        projectDao.deleteRisk(risk)
    }

    suspend fun insertAuditLog(action: String, userRole: String, details: String) {
        val log = AuditLog(action = action, userRole = userRole, details = details)
        projectDao.insertAuditLog(log)
    }

    suspend fun insertAuditLog(auditLog: AuditLog) {
        projectDao.insertAuditLog(auditLog)
    }

    suspend fun getUserByEmail(email: String): com.example.data.model.User? {
        return projectDao.getUserByEmail(email)
    }

    suspend fun insertUser(user: com.example.data.model.User): Long {
        return projectDao.insertUser(user)
    }
}
