package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Project
import com.example.data.model.ProjectTask
import com.example.data.model.RiskItem
import com.example.data.repository.ProjectRepository
import com.example.network.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object ProjectMgmt : Screen()
    object Chat : Screen()
    object Visualization : Screen()
    object Reports : Screen()
    object AuditLogView : Screen()
    object UserRoles : Screen()
}

data class ChatMessage(
    val sender: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel(
    application: Application,
    private val repository: ProjectRepository,
    private val crashService: com.example.util.CrashMonitoringService
) : AndroidViewModel(application) {

    val projects: StateFlow<List<Project>>
    val auditLogs: StateFlow<List<AuditLog>>

    // Uncaught Crash Monitoring & App Telemetry Status Flows
    val systemCrashes: StateFlow<List<com.example.util.CrashLog>> = crashService.crashes
    val performanceMetrics: StateFlow<com.example.util.PerformanceMetrics> = crashService.performanceMetrics

    fun clearCrashReports() {
        crashService.clearCrashes()
        logActivity("Security Maintenance", "ล้างบันทึกประวัติการแครชของระบบทั้งหมด")
    }

    fun triggerSimulatedCrash(message: String) {
        val ex = RuntimeException("Simulated Sandbox Crash: $message")
        crashService.logCrash(ex)
        logActivity("System Monitor", "จำลองการเกิดบั๊ก / Exception: ${ex.message}")
    }

    // Navigation and Active Selection
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    // Interactive tasks & risks for selected project
    val projectTasks: StateFlow<List<ProjectTask>> = _selectedProject
        .flatMapLatest { project ->
            if (project != null) repository.getTasksForProject(project.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectRisks: StateFlow<List<RiskItem>> = _selectedProject
        .flatMapLatest { project ->
            if (project != null) repository.getRisksForProject(project.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chatbot State
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Analysis Loading States
    private val _analysisStatus = MutableStateFlow<Map<String, String>>(emptyMap()) // Category -> Status ("Idle", "Running", "Completed", "Failed")
    val analysisStatus: StateFlow<Map<String, String>> = _analysisStatus.asStateFlow()

    // Secure Enterprise Gateway Proxy routing state
    private val _useEnterpriseGateway = MutableStateFlow(GeminiApiClient.useEnterpriseGateway)
    val useEnterpriseGateway: StateFlow<Boolean> = _useEnterpriseGateway.asStateFlow()

    private val _enterpriseGatewayUrl = MutableStateFlow(GeminiApiClient.enterpriseGatewayUrl)
    val enterpriseGatewayUrl: StateFlow<String> = _enterpriseGatewayUrl.asStateFlow()

    private val _enterpriseGatewayToken = MutableStateFlow(GeminiApiClient.enterpriseGatewayToken)
    val enterpriseGatewayToken: StateFlow<String> = _enterpriseGatewayToken.asStateFlow()

    fun setUseEnterpriseGateway(enabled: Boolean) {
        _useEnterpriseGateway.value = enabled
        GeminiApiClient.useEnterpriseGateway = enabled
        viewModelScope.launch {
            logActivity("Security Configuration", "เปลี่ยนโหมดเส้นทาง AI: " + if (enabled) "เซิร์ฟเวอร์เกตเวย์องค์กร (Secure Backend Proxy)" else "เชื่อมตรงผ่านโมเดล Client SDK")
        }
    }

    fun setEnterpriseGatewayUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty()) {
            _enterpriseGatewayUrl.value = trimmed
            GeminiApiClient.enterpriseGatewayUrl = trimmed
        }
    }

    fun setEnterpriseGatewayToken(token: String) {
        val trimmed = token.trim()
        _enterpriseGatewayToken.value = trimmed
        GeminiApiClient.enterpriseGatewayToken = trimmed
    }

    // Auth & Role Simulation State
    private val _userRole = MutableStateFlow("System Analyst") // "Admin", "Manager", "System Analyst", "Developer", "QA", "Viewer"
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Auth error and success states for real authentication
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _registerSuccess = MutableStateFlow<Boolean>(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess.asStateFlow()

    // Quick Notifications
    private val _notifications = MutableStateFlow<List<String>>(listOf(
        "ยินดีต้อนรับเข้าสู่ระบบวิเคราะห์ Enterprise AI Analyst Platform",
        "ระบบตรวจสอบ: สถาปัตยกรรม FinTech Mobile Wallet สอดคล้องตามมาตรฐาน PCI-DSS"
    ))
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    val isApiKeyAvailable: Boolean = GeminiApiClient.isApiKeyAvailable()

    init {
        projects = repository.allProjects.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        auditLogs = repository.allAuditLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Seed initial project and user data if empty
        viewModelScope.launch(Dispatchers.IO) {
            val defaultUser = repository.getUserByEmail("demo@enterprise.com")
            if (defaultUser == null) {
                val salt = com.example.util.HashUtils.generateSalt()
                val hashedPassword = com.example.util.HashUtils.hashPassword("password123", salt)
                repository.insertUser(com.example.data.model.User(
                    email = "demo@enterprise.com",
                    passwordHash = hashedPassword,
                    salt = salt,
                    role = "System Analyst"
                ))
            }

            projects.take(2).collect { list ->
                if (list.isEmpty()) {
                    seedDefaultProjects()
                } else if (_selectedProject.value == null && list.isNotEmpty()) {
                    _selectedProject.value = list.first()
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        viewModelScope.launch {
            _currentScreen.value = screen
            logActivity("Navigate", "เปลี่ยนหน้าจอไปยัง: ${screen::class.simpleName}")
        }
    }

    fun selectProject(project: Project) {
        viewModelScope.launch {
            _selectedProject.value = project
            logActivity("Select Project", "เลือกโครงการวิเคราะห์: ${project.name}")
        }
    }

    fun changeUserRole(role: String) {
        viewModelScope.launch {
            _userRole.value = role
            logActivity("Permission Change", "เปลี่ยนบทบาทผู้ใช้จำลองเป็น: $role")
            addNotification("คุณได้สลับบทบาทการใช้งานเป็น: $role")
        }
    }

    fun login(email: String, passwordEntered: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            val emailClean = email.trim()
            if (emailClean.isEmpty()) {
                _authError.value = "โปรดกรอกอีเมลผู้ใช้งาน"
                return@launch
            }
            if (passwordEntered.isEmpty()) {
                _authError.value = "โปรดกรอกรหัสผ่าน"
                return@launch
            }
            val user = repository.getUserByEmail(emailClean)
            if (user == null) {
                _authError.value = "ไม่พบบัญชีผู้ใช้งานนี้ในระบบ โปรดสมัครสมาชิกใหม่"
                return@launch
            }

            // Check if password matches (supporting fallback for older unhashed legacy entries)
            val isMatch = if (user.salt.isEmpty()) {
                user.passwordHash == passwordEntered
            } else {
                val hashedCompare = com.example.util.HashUtils.hashPassword(passwordEntered, user.salt)
                user.passwordHash == hashedCompare
            }

            if (!isMatch) {
                _authError.value = "รหัสผ่านไม่ถูกต้อง โปรดลองอีกครั้ง"
                return@launch
            }

            withContext(Dispatchers.Main) {
                _userRole.value = user.role
                _isLoggedIn.value = true
                _currentScreen.value = Screen.Dashboard
                logActivity("Login", "เข้าสู่ระบบสำเร็จด้วยบัญชี ${user.email} (สิทธิ์: ${user.role})")
                addNotification("เข้าสู่ระบบเรียบร้อยแล้ว ยินดีต้อนรับคุณ ${user.email}")
            }
        }
    }

    fun register(email: String, passwordEntered: String, role: String = "System Analyst") {
        viewModelScope.launch(Dispatchers.IO) {
            _authError.value = null
            _registerSuccess.value = false
            val emailClean = email.trim()
            if (emailClean.isEmpty() || passwordEntered.isEmpty()) {
                _authError.value = "โปรดกรอกข้อมูลอีเมลและรหัสผ่านให้ครบถ้วน"
                return@launch
            }
            val existing = repository.getUserByEmail(emailClean)
            if (existing != null) {
                _authError.value = "อีเมลนี้ได้รับการลงทะเบียนแล้ว ไม่สามารถใช้ซ้ำได้"
                return@launch
            }

            // Secure Hashing with dynamic salt per user
            val salt = com.example.util.HashUtils.generateSalt()
            val hashedPassword = com.example.util.HashUtils.hashPassword(passwordEntered, salt)

            val newUser = com.example.data.model.User(
                email = emailClean,
                passwordHash = hashedPassword,
                salt = salt,
                role = role
            )
            val result = repository.insertUser(newUser)
            if (result > -1) {
                _registerSuccess.value = true
                _authError.value = null
                logActivity("User Register", "ลงทะเบียนบัญชีใหม่สำเร็จ: $emailClean")
                addNotification("ลงทะเบียนสมัครสมาชิกสำเร็จ: $emailClean")
            } else {
                _authError.value = "เกิดข้อผิดพลาดในการบันทึกข้อมูลผู้ใช้งาน"
            }
        }
    }

    fun clearAuthStates() {
        _authError.value = null
        _registerSuccess.value = false
    }

    fun logout() {
        viewModelScope.launch {
            _isLoggedIn.value = false
            _currentScreen.value = Screen.Login
            logActivity("Logout", "ออกจากระบบสำเร็จ")
        }
    }

    fun addNotification(message: String) {
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, message)
        _notifications.value = currentList
    }

    fun logActivity(action: String, details: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAuditLog(action, _userRole.value, details)
        }
    }

    // Dynamic Task Controls (CRUD)
    fun addTask(title: String, description: String, priority: String, assignee: String, sprint: String) {
        val projectId = _selectedProject.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val task = ProjectTask(
                projectId = projectId,
                title = title,
                description = description,
                status = "Todo",
                priority = priority,
                assignee = assignee,
                sprint = sprint
            )
            repository.insertTask(task)
            logActivity("Create Task", "เพิ่มงานใหม่: $title [โครงการ ID: $projectId]")
            addNotification("สร้างงานสำเร็จ: $title")
        }
    }

    fun updateTaskStatus(task: ProjectTask, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(status = newStatus)
            repository.updateTask(updated)
            logActivity("Update Task", "ย้ายงาน '${task.title}' ไปยังสถานะ: $newStatus")
        }
    }

    fun deleteTask(task: ProjectTask) {
        if (!hasWritePermission()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
            logActivity("Delete Task", "ลบงาน: ${task.title}")
            addNotification("ลบงานสำเร็จ: ${task.title}")
        }
    }

    // Dynamic Risk Controls
    fun addRisk(title: String, severity: String, category: String, status: String, mitigationPlan: String) {
        val projectId = _selectedProject.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val risk = RiskItem(
                projectId = projectId,
                title = title,
                severity = severity,
                category = category,
                status = status,
                mitigationPlan = mitigationPlan
            )
            repository.insertRisk(risk)
            logActivity("Create Risk", "เพิ่มความเสี่ยงใหม่: $title")
            addNotification("ตรวจพบความเสี่ยงใหม่: $title")
        }
    }

    fun deleteRisk(risk: RiskItem) {
        if (!hasWritePermission()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRisk(risk)
            logActivity("Delete Risk", "ลบความเสี่ยง: ${risk.title}")
        }
    }

    // AI Analysis Process
    fun runAnalysisForCategory(category: String) {
        val project = _selectedProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Update Status
            val statusMap = _analysisStatus.value.toMutableMap()
            statusMap[category] = "Running"
            _analysisStatus.value = statusMap

            logActivity("AI Analysis", "เริ่มการวิเคราะห์ด้าน $category สำหรับ ${project.name}")

            try {
                val reportText = GeminiApiClient.generateAnalysis(
                    rawContent = project.rawContent,
                    category = category,
                    projectName = project.name,
                    industry = project.industry
                )

                // Save report back to Project
                val updatedProject = when (category) {
                    "Requirements" -> project.copy(requirementsReport = reportText)
                    "Business" -> project.copy(businessReport = reportText)
                    "System" -> project.copy(systemReport = reportText)
                    "Database" -> project.copy(databaseReport = reportText)
                    "API" -> project.copy(apiReport = reportText)
                    "Security" -> project.copy(securityReport = reportText)
                    "Performance" -> project.copy(performanceReport = reportText)
                    "Code" -> project.copy(codeReport = reportText)
                    "Infrastructure" -> project.copy(infraReport = reportText)
                    "Risk" -> project.copy(riskReport = reportText)
                    else -> project
                }

                repository.updateProject(updatedProject)
                _selectedProject.value = updatedProject

                val finalStatusMap = _analysisStatus.value.toMutableMap()
                finalStatusMap[category] = "Completed"
                _analysisStatus.value = finalStatusMap

                logActivity("AI Analysis Success", "วิเคราะห์ด้าน $category สำเร็จ")
                addNotification("วิเคราะห์ด้าน $category สำหรับ '${project.name}' เรียบร้อยแล้ว!")

                // Auto parse and seed tasks or risks if Database or Risk reports are generated
                if (category == "Risk" && reportText.contains("ความเสี่ยง")) {
                    seedRisksFromAI(project.id, reportText)
                }

            } catch (e: Exception) {
                Log.e("AppViewModel", "Analysis Error: ", e)
                val finalStatusMap = _analysisStatus.value.toMutableMap()
                finalStatusMap[category] = "Failed"
                _analysisStatus.value = finalStatusMap

                // Save error details to the project report so it renders beautifully in the UI
                val errorMsg = "### ❌ การวิเคราะห์ล้มเหลว (Analysis Failed)\n\n**เหตุผลความล้มเหลว:**\n${e.message ?: "ข้อผิดพลาดที่ไม่รู้จัก (Unknown Error)"}\n\n*โปรดตรวจสอบการกำหนดค่าความปลอดภัยหรือ API Gateway ของท่าน*"
                val updatedProject = when (category) {
                    "Requirements" -> project.copy(requirementsReport = errorMsg)
                    "Business" -> project.copy(businessReport = errorMsg)
                    "System" -> project.copy(systemReport = errorMsg)
                    "Database" -> project.copy(databaseReport = errorMsg)
                    "API" -> project.copy(apiReport = errorMsg)
                    "Security" -> project.copy(securityReport = errorMsg)
                    "Performance" -> project.copy(performanceReport = errorMsg)
                    "Code" -> project.copy(codeReport = errorMsg)
                    "Infrastructure" -> project.copy(infraReport = errorMsg)
                    "Risk" -> project.copy(riskReport = errorMsg)
                    else -> project
                }
                _selectedProject.value = updatedProject
                addNotification("การวิเคราะห์ด้าน $category ล้มเหลว: ${e.message ?: "ข้อผิดพลาดที่ไม่รู้จัก"}")
            }
        }
    }

    // AI Chatbot Logic
    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        val currentProject = _selectedProject.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = ChatMessage(sender = "user", content = text)
            _chatHistory.value = _chatHistory.value + userMsg
            _isChatLoading.value = true

            logActivity("AI Chat", "ส่งคำถามไปยัง AI Assistant: $text")

            val systemContext = """
                You are an expert AI Software Architect. You are helping analyze the project:
                Name: ${currentProject.name}
                Industry: ${currentProject.industry}
                Description: ${currentProject.description}
                
                Please reply in Thai, using markdown format, with structured bullets, tables, or ASCII diagrams if requested. Always maintain the expert architect persona.
                
                Project Technical Specifications:
                ${currentProject.rawContent}
            """.trimIndent()

            try {
                val responseText = GeminiApiClient.generateAnalysis(
                    rawContent = systemContext + "\n\nUser Question: $text",
                    category = "Chat",
                    projectName = currentProject.name,
                    industry = currentProject.industry
                )

                val assistantMsg = ChatMessage(sender = "assistant", content = responseText)
                _chatHistory.value = _chatHistory.value + assistantMsg
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    sender = "assistant",
                    content = "ขออภัยด้วยครับ มีข้อผิดพลาดเกิดขึ้นในการสื่อสารกับ AI: ${e.message}"
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
    }

    // Custom File/Project Upload Emulator
    fun uploadNewProject(name: String, industry: String, description: String, rawContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newProject = Project(
                name = name,
                industry = industry,
                description = description,
                rawContent = rawContent
            )
            val id = repository.insertProject(newProject)
            val savedProject = newProject.copy(id = id.toInt())
            _selectedProject.value = savedProject
            logActivity("Upload Project", "อัปโหลดโปรเจกต์ใหม่: ${name}สำเร็จ")
            addNotification("อัปโหลดโปรเจกต์ใหม่สำเร็จ: $name")

            // Seed mock tasks for the uploaded project to avoid empty dashboard
            seedTasksForProject(savedProject.id)
        }
    }

    // Permission Checking based on RBAC Roles
    fun hasWritePermission(): Boolean {
        return when (_userRole.value) {
            "Admin", "System Analyst", "Project Manager" -> true
            else -> false
        }
    }

    fun hasAdminPermission(): Boolean {
        return _userRole.value == "Admin"
    }

    private suspend fun seedRisksFromAI(projectId: Int, text: String) {
        // Simple automatic parsing of risks from AI output or seed default risks for realism
        repository.insertRisk(RiskItem(
            projectId = projectId,
            title = "ความปลอดภัยของสิทธิ์ผู้ใช้ (Broken Access Control)",
            severity = "High",
            category = "Security",
            status = "Identified",
            mitigationPlan = "ออกแบบ Custom Checkpoint Middleware ใน API Gateway ตรวจสอบ JWT Token และ Role Scope ทุกครั้ง"
        ))
        repository.insertRisk(RiskItem(
            projectId = projectId,
            title = "API Response Rate Bottleneck",
            severity = "Medium",
            category = "Technical",
            status = "Mitigating",
            mitigationPlan = "ตั้งค่า Redis Rate Limiter ที่ Kong Gateway"
        ))
    }

    private suspend fun seedTasksForProject(projectId: Int) {
        repository.insertTask(ProjectTask(
            projectId = projectId,
            title = "ออกแบบ Architecture Diagram & Database Schema",
            description = "สรุปและวาดแอร์เรย์สคีมาฐานข้อมูลร่วมกับ ERD เพื่อส่งงานลูกค้า",
            status = "In Progress",
            priority = "High",
            assignee = "System Analyst",
            sprint = "Sprint 1"
        ))
        repository.insertTask(ProjectTask(
            projectId = projectId,
            title = "ตั้งค่าโครงสร้างโปรเจกต์ Next.js 15 และ FastAPI",
            description = "เซ็ตอัพ Environment Boilerplate และ Docker Compose",
            status = "Todo",
            priority = "Medium",
            assignee = "Developer",
            sprint = "Sprint 1"
        ))
        repository.insertTask(ProjectTask(
            projectId = projectId,
            title = "เขียนคำอธิบาย REST API Endpoints",
            description = "ส่งสเปค OpenAPI สรุปให้ทีม QA ไปเตรียมโมเดลการทดสอบ",
            status = "Todo",
            priority = "Low",
            assignee = "QA",
            sprint = "Sprint 1"
        ))
    }

    private suspend fun seedDefaultProjects() {
        val fintech = Project(
            name = "FinTech Mobile Wallet",
            industry = "Banking / FinTech",
            description = "สัญญาระบบกระเป๋าเงินอิเล็กทรอนิกส์สำหรับรองรับผู้ใช้งานระดับล้านคน มีฟังก์ชันโอนเงิน เติมเงิน ถอนเงิน และเชื่อมต่อระบบธนาคารหลัก มีมาตรการความปลอดภัยและประมวลผลธุรกรรมรวดเร็ว",
            rawContent = """
                # FinTech Mobile Wallet System Specification
                
                ## Introduction
                ระบบกระเป๋าเงินอิเล็กทรอนิกส์ (E-Wallet) สำหรับองค์กรการเงินขนาดใหญ่ รองรับลูกค้า 1,000,000+ บัญชี พร้อมความปลอดภัยระดับธนาคาร (PCI-DSS) และสถาปัตยกรรมแบบ Microservices
                
                ## Core Modules
                1. Account & Security (Authentication, RBAC, KYC)
                2. Balance & Ledger (Real-time Transaction, Ledger Logging)
                3. Bank Integration Gateway (ISO 8583 protocol, Bank Transfer)
                4. Notification Engine (SMS, Push Notification)
                
                ## Requirements
                - FR-01: ผู้ใช้สามารถโอนเงินหากันได้ภายใน 500ms
                - FR-02: ตรวจสอบย้อนหลังทุกธุรกรรมด้วย Ledger Immutable History
                - NFR-01: ความปลอดภัยแบบ OAuth 2.0 ร่วมกับ Hardware Security Module (HSM)
                - NFR-02: อัตราการรองรับธุรกรรมสูงสุด 1,500 TPS (Transactions Per Second)
            """.trimIndent()
        )

        val ecommerce = Project(
            name = "Enterprise E-Commerce API",
            industry = "E-Commerce / SaaS",
            description = "สถาปัตยกรรมระบบร้านค้าออนไลน์ขนาดใหญ่ รองรับการสั่งซื้อพร้อมกันสูง ตะกร้าสินค้า ตัดยอดสินค้าคงคลัง และระบบชำระเงินพ่วงต่อกับ Stripe Gateway",
            rawContent = """
                # Enterprise E-Commerce Blueprint
                
                ## Core Modules
                1. Catalog Management (Search, Categories, Dynamic Pricing)
                2. Order Engine (Cart, Checkout flow, Invoice generator)
                3. Inventory & Warehouse (FIFO stock management, Low stock alerts)
                4. Stripe & Credit Card Integration
                
                ## Tech Stack
                - Frontend: Next.js + Tailwind
                - Backend: FastAPI + Celery Workers
                - Database: PostgreSQL (Primary), Redis (Caching)
            """.trimIndent()
        )

        val fintechId = repository.insertProject(fintech).toInt()
        val ecommerceId = repository.insertProject(ecommerce).toInt()

        seedTasksForProject(fintechId)
        seedTasksForProject(ecommerceId)

        // Seed Audit Logs
        repository.insertAuditLog(AuditLog(action = "System Init", userRole = "System Analyst", details = "เตรียมฐานข้อมูลตัวอย่าง FinTech และ E-Commerce สำเร็จ"))
    }
}
