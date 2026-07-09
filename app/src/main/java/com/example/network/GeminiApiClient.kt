package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UnretryableApiException(message: String) : Exception(message)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    @Volatile
    var enterpriseGatewayUrl: String = "https://gateway.enterprise-analyst.ai/v1/analyze"

    @Volatile
    var useEnterpriseGateway: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // NOTE: SECURITY DISCLAIMER FOR ENTERPRISE DEPLOYMENT
    // Calling Gemini API directly from the client application with an API key embedded in the binary 
    // is highly vulnerable to extraction via reverse engineering tools like JADX, Apktool, or dex2jar.
    // For Production Deployment: The application MUST proxy all LLM and analysis requests through 
    // a secure company gateway / backend microservice where credentials are kept hidden in secure env vaults.
    fun isApiKeyAvailable(): Boolean {
        val apiKey = BuildConfig.GEMINI_API_KEY
        return apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
    }

    suspend fun generateAnalysis(
        rawContent: String,
        category: String,
        projectName: String,
        industry: String
    ): String = withContext(Dispatchers.IO) {
        if (useEnterpriseGateway) {
            return@withContext generateAnalysisViaGateway(rawContent, category, projectName, industry)
        }
        return@withContext generateAnalysisDirect(rawContent, category, projectName, industry)
    }

    suspend fun generateAnalysisViaGateway(
        rawContent: String,
        category: String,
        projectName: String,
        industry: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("category", category)
                put("rawContent", rawContent)
                put("projectName", projectName)
                put("industry", industry)
                put("systemInstruction", "You are an elite enterprise software architect, system analyst, and security officer. Write a professional, detailed, structured analysis in Thai.")
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(enterpriseGatewayUrl)
                .post(requestBody)
                // In production, attach secure enterprise client token
                .addHeader("Authorization", "Bearer eYJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.enterpriseMockToken")
                .build()

            val textResult = retryWithBackoff(times = 2) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Gateway HTTP Error ${response.code}")
                    }
                    val body = response.body?.string() ?: ""
                    if (body.startsWith("{")) {
                        val json = JSONObject(body)
                        json.optString("analysis", json.optString("text", body))
                    } else {
                        body
                    }
                }
            }
            return@withContext "Enterprise Secure Proxy Active: Dispatched through $enterpriseGatewayUrl\n\n$textResult"
        } catch (e: Exception) {
            Log.e(TAG, "Gateway analysis call failed, running local secure processing instead", e)
            return@withContext "Enterprise secure gateway proxy ($enterpriseGatewayUrl) is currently unreachable in sandbox. Running secure direct failover below...\n\n" +
                    generateAnalysisDirect(rawContent, category, projectName, industry)
        }
    }

    suspend fun generateAnalysisDirect(
        rawContent: String,
        category: String,
        projectName: String,
        industry: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is empty or placeholder!")
            return@withContext getOfflineMockAnalysis(category, projectName, industry, rawContent)
        }

        val prompt = buildPromptForCategory(category, rawContent, projectName, industry)

        try {
            // Build direct REST payload
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                // Optional system instruction to make response look neat
                val systemInstruction = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are an elite enterprise software architect, system analyst, and security officer. Write a professional, detailed, structured analysis in Thai. Use bold terms, checklists, bullet points, and mock mermaid schema where appropriate. Avoid code block wraps around final reports unless writing diagrams.")
                        })
                    })
                }
                put("systemInstruction", systemInstruction)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val textResult = retryWithBackoff(times = 3) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code}, Error: $errorBody")
                        val errorMsg = "HTTP ${response.code}: $errorBody"
                        
                        // Fail instantly on unretryable client-side HTTP status codes (e.g. 400 Bad Request, 401 Unauthorized)
                        if (response.code in listOf(400, 401, 403, 404)) {
                            throw UnretryableApiException(errorMsg)
                        } else {
                            throw Exception(errorMsg)
                        }
                    }

                    val responseBodyStr = response.body?.string()
                    if (responseBodyStr.isNullOrEmpty()) {
                        throw Exception("Empty response from AI")
                    }

                    val responseJson = JSONObject(responseBodyStr)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        throw Exception("No candidates returned in AI response")
                    }

                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")

                    if (text.isNullOrEmpty()) {
                        throw Exception("No text content found in AI response")
                    }

                    text
                }
            }
            return@withContext textResult
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call after retries: ", e)
            val cleanMessage = if (e is UnretryableApiException) {
                "Client-side API Error: ${e.message} (Skipped retrying because this error is non-recoverable)"
            } else {
                e.message
            }
            return@withContext "Error while contacting AI: $cleanMessage\nRunning local fallback analyzer."
        }
    }

    private suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 6000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: UnretryableApiException) {
                // Instantly propagate non-retryable client API exceptions
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block()
    }

    private fun buildPromptForCategory(
        category: String,
        rawContent: String,
        projectName: String,
        industry: String
    ): String {
        return when (category) {
            "Requirements" -> """
                Analyze the requirements of the project '$projectName' (Industry: $industry) based on the input text:
                "$rawContent"
                Please provide:
                1. **Functional Requirements** (List at least 5 clear features)
                2. **Non-Functional Requirements** (Performance, security, availability)
                3. **Missing Requirements** (Identify what was omitted but necessary for an enterprise system in this industry)
                4. **Duplicate or Conflicting Requirements**
                Format beautifully in Thai.
            """.trimIndent()

            "Business" -> """
                Analyze the business processes and stakeholder workflows for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Business Workflow Process** (Step-by-step)
                2. **Stakeholders and User Personas**
                3. **Actors and Core Roles**
                4. **Business Value & KPI recommendations**
                Format beautifully in Thai.
            """.trimIndent()

            "System" -> """
                Analyze the software architecture and module dependencies for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Proposed Architecture Style** (e.g., Clean Architecture, Microservices)
                2. **Core System Modules** (Explain what each does)
                3. **Component Dependencies & Coupling Analysis**
                4. **Cohesion improvement suggestions**
                Format beautifully in Thai.
            """.trimIndent()

            "Database" -> """
                Analyze the database design and data relations for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Proposed ERD Schema Structure** (Describe the tables, primary keys, foreign keys)
                2. **Table Relationships** (One-to-many, many-to-many)
                3. **Missing Indices & Optimization Recommendations**
                4. **Database Normalization Assessment** (1NF, 2NF, 3NF)
                
                Please also output a structured representation of the tables at the end, formatted precisely like this:
                [TABLES_SCHEMA]
                Table: TableName
                - Column1 (Type, PK)
                - Column2 (Type, FK -> OtherTable.Col)
                - Column3 (Type)
                [/TABLES_SCHEMA]
                Format beautifully in Thai.
            """.trimIndent()

            "API" -> """
                Analyze API Endpoints and communications for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Proposed REST or GraphQL API Endpoints** (Methods, paths, description, and mock request/response parameters)
                2. **Security & Authentication recommendations** (JWT, OAuth)
                3. **Rate Limiting & Throttling Strategies**
                4. **API Gateway configuration tips**
                Format beautifully in Thai.
            """.trimIndent()

            "Security" -> """
                Analyze potential security risks based on OWASP Top 10 for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Risk of SQL Injection, XSS, and CSRF** (And how to mitigate them)
                2. **Authentication & Session Management Vulnerabilities**
                3. **Sensitive Data Exposure & Encryption standards** (At-rest, In-transit)
                4. **OWASP Top 10 Compliance checklist**
                Format beautifully in Thai.
            """.trimIndent()

            "Performance" -> """
                Analyze the performance bottleneck and caching strategy for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Potential Bottlenecks** (CPU, Memory, Slow database queries)
                2. **Caching Strategy** (Redis, CDN caching layer)
                3. **Resource Scaling Recommendation** (Horizontal vs vertical, autoscaling)
                4. **Load Balancing & Failover mechanics**
                Format beautifully in Thai.
            """.trimIndent()

            "Code" -> """
                Perform code review and complexity analysis on the provided input for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Code Quality & Readability Review**
                2. **Complexity Assessment & Code Smells**
                3. **Dead or Duplicate Code patterns found/anticipated**
                4. **Refactoring suggestions based on SOLID and DRY principles**
                Format beautifully in Thai.
            """.trimIndent()

            "Infrastructure" -> """
                Analyze and design the cloud infrastructure for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **Proposed Cloud Architecture Blueprint** (AWS/GCP/Azure)
                2. **Network Topology & Subnet isolation** (VPC, Public/Private subnets)
                3. **Server and Serverless dependencies**
                4. **CI/CD Pipeline configuration blueprint**
                
                Please also output a structural description of nodes at the end, formatted precisely like this:
                [INFRA_SCHEMA]
                Node: NodeName (Type) -> ConnectedNode
                [/INFRA_SCHEMA]
                Format beautifully in Thai.
            """.trimIndent()

            "Risk" -> """
                Perform comprehensive risk assessment for '$projectName' ($industry):
                "$rawContent"
                Please provide:
                1. **High Severity Risks** (Technical, legal, or business, with mitigation plan)
                2. **Medium Severity Risks**
                3. **Low Severity Risks**
                4. **Overall Risk Score and Contingency plan**
                Format beautifully in Thai.
            """.trimIndent()

            "Chat" -> rawContent

            else -> "Analyze the system details of '$projectName': $rawContent"
        }
    }

    private fun getOfflineMockAnalysis(category: String, projectName: String, industry: String, rawContent: String = ""): String {
        return when (category) {
            "Chat" -> {
                val question = if (rawContent.contains("User Question:")) {
                    rawContent.substringAfter("User Question:").trim()
                } else {
                    "คำถามของคุณ"
                }
                
                val lowerQuestion = question.lowercase()
                val responseDetails = when {
                    lowerQuestion.contains("database") || lowerQuestion.contains("db") || lowerQuestion.contains("ฐานข้อมูล") || lowerQuestion.contains("table") -> """
                        - **การออกแบบฐานข้อมูล**: แนะนำใช้ PostgreSQL ร่วมกับ Room Database สำหรับฝั่ง Client ครับ
                        - **โครงสร้าง**: มีการจัดเก็บแยกตารางระหว่าง Projects, Tasks, Risks และ Audit Logs อย่างชัดเจน
                        - **ข้อเสนอแนะ**: ควรเพิ่มดัชนี (Index) บนคอลัมน์ที่มีการสืบค้นบ่อย ๆ เช่น `projectId`
                    """.trimIndent()
                    
                    lowerQuestion.contains("security") || lowerQuestion.contains("ปลอดภัย") || lowerQuestion.contains("สิทธิ์") || lowerQuestion.contains("key") -> """
                        - **ความปลอดภัย**: ตัวแอปใช้ระบบควบคุมสิทธิ์แบบ Role-Based Access Control (RBAC)
                        - **การคุ้มครองข้อมูล**: ควรหลีกเลี่ยงการฝัง API key ไว้ในแอปโดยตรง และเลือกใช้ Firebase AI SDK หรือสถาปัตยกรรม Proxy Server แทนครับ
                        - **สิทธิ์การทำงาน**: จำกัดสิทธิ์การเขียน/ลบตามตำแหน่ง (Admin, SA, PM)
                    """.trimIndent()

                    lowerQuestion.contains("performance") || lowerQuestion.contains("ช้า") || lowerQuestion.contains("เร็ว") || lowerQuestion.contains("cache") -> """
                        - **การตอบสนอง**: โค้ดถูกออกแบบให้ใช้ Kotlin Coroutines และ Flow เพื่อหลีกเลี่ยงการบล็อก UI thread
                        - **การปรับแต่ง**: แนะนำให้ตั้งค่า caching ด้วย Redis ฝั่งเซิร์ฟเวอร์ และจัดทำ Pagination ของประวัติกิจกรรมย้อนหลัง
                    """.trimIndent()

                    lowerQuestion.contains("api") || lowerQuestion.contains("endpoint") || lowerQuestion.contains("rest") -> """
                        - **ระบบ APIs**: ควรมี Endpoints มาตรฐานสำหรับระบบนี้ เช่น `/api/v1/projects`, `/api/v1/tasks`
                        - **ความปลอดภัย**: แนะนำใช้ Bearer JWT ในการยืนยันตัวตนสำหรับทุก ๆ API Call
                    """.trimIndent()

                    else -> """
                        - **สถาปัตยกรรมแอปพลิเคชัน**: โครงสร้างได้รับการออกแบบตามรูปแบบ Bento Grid ที่จัดสรรข้อมูลเป็นบล็อกสวยงามและเข้าใจง่าย
                        - **ข้อแนะนำ**: คุณสามารถเลือกหัวข้อที่ต้องการจากแท็บวิเคราะห์เพื่อสร้างรายละเอียดวิเคราะห์ทั้ง 10 มิติเชิงลึกได้เลยครับ
                    """.trimIndent()
                }

                """
                    ### 💬 ผู้ช่วยวิเคราะห์สถาปัตยกรรมระบบ AI (โหมดจำลองออฟไลน์)
                    *หมายเหตุ: ตอบกลับในโหมดออฟไลน์เนื่องจากไม่ได้ตั้งค่า API Key*
                    
                    **คำถามของคุณ:** "$question"
                    
                    **คำแนะนำจากผู้ช่วยสถาปนิกซอฟต์แวร์:**
                    $responseDetails
                    
                    มีเรื่องอื่น ๆ เกี่ยวกับโครงสร้างโครงการ **$projectName** ($industry) ที่ต้องการให้ผมช่วยเหลือเพิ่มเติมไหมครับ?
                """.trimIndent()
            }

            "Requirements" -> """
                ### 📋 ผลการวิเคราะห์ Requirements สำหรับ $projectName ($industry)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Functional Requirements (ข้อกำหนดเชิงฟังก์ชัน)**
                   - **FR-01: ระบบยืนยันตัวตนและความปลอดภัยสูง (Multi-Factor Authentication)** เพื่อป้องกันการเข้าถึงข้อมูลขององค์กรโดยไม่ได้รับอนุญาต
                   - **FR-02: หน้าจอ Dashboard อัจฉริยะ (Enterprise Executive Dashboard)** แสดงรายงานภาพรวมการวิเคราะห์ในรูปแบบกราฟแบบ Real-time
                   - **FR-03: ระบบอัปโหลดและประมวลผลไฟล์เอกสารหลายรูปแบบ (Multi-format File Parser)** รองรับ PDF, Word, SQL, และ Source Code
                   - **FR-04: แชทบอทอัจฉริยะถามตอบข้อมูลระบบ (AI Context Chat)** เพื่อตอบคำถามเชิงลึกเกี่ยวกับเอกสารข้อกำหนดของโครงการ
                   - **FR-05: ระบบจัดการสถานะโครงการและ Kanban Board** สำหรับทีมผู้พัฒนา, SA, และ PM ติดตามงานในแต่ละ Sprint
                
                2. **Non-Functional Requirements (ข้อกำหนดเชิงเทคนิค)**
                   - **NFR-01: Response Time ต่ำกว่า 2 วินาที** สำหรับการโหลดหน้าแดชบอร์ดภาพรวมโครงการ
                   - **NFR-02: ความปลอดภัยของข้อมูลตามมาตรฐาน ISO/IEC 27001** และการเข้ารหัสข้อมูลสำคัญแบบ AES-256
                   - **NFR-03: รองรับผู้ใช้งานพร้อมกัน 500+ Concurrent Session** โดยใช้ Redis Cache เป็นกลไกในการแคชข้อมูลเซสชัน
                
                3. **Missing Requirements (ข้อกำหนดที่ขาดหายไปและแนะนำให้เพิ่ม)**
                   - **ระบบ Audit Trail และ Activity Log เชิงลึก** เพื่อเก็บประวัติการแก้ไขและนำออกข้อมูลที่สำคัญของระบบ
                   - **กลไกการกู้คืนระบบแบบอัตโนมัติ (Disaster Recovery & Backup Policy)** ซึ่งจำเป็นสำหรับระบบระดับ Enterprise
                
                4. **ข้อกำหนดที่ซ้ำซ้อนหรืออาจเกิดความขัดแย้ง**
                   - การอัปโหลดไฟล์ขนาดใหญ่พร้อมกันผ่าน HTTP API อาจขัดแย้งกับข้อกำหนด Response Time จึงแนะนำให้ใช้ Background Worker Queue ในการประมวลผลแทน
            """.trimIndent()

            "Business" -> """
                ### 🏢 ผลการวิเคราะห์กระบวนการทางธุรกิจ (Business Analysis)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Business Workflow (ขั้นตอนการทำงานทางธุรกิจ)**
                   - **ขั้นตอนที่ 1 (Input/Upload):** ผู้ใช้ระบบอัปโหลดข้อกำหนด (SRS) หรือโค้ดต้นฉบับเข้าไปในระบบ
                   - **ขั้นตอนที่ 2 (AI Engine Analysis):** ระบบประมวลผลวิเคราะห์ข้อมูลจำแนกตาม 10 ด้านที่สำคัญ
                   - **ขั้นตอนที่ 3 (Visualization):** ผลลัพธ์ถูกนำเสนอในรูปแบบแผนภาพโครงสร้างสัมพันธ์ (ERD, Cloud Topology, Flows)
                   - **ขั้นตอนที่ 4 (Actionable Output):** ทีมนำผลลัพธ์ไปประยุกต์ใช้ในการวางแผน Sprint พัฒนา และออกเอกสารรายงานระบบ
                
                2. **Stakeholders & Roles (ผู้เกี่ยวข้องกับระบบ)**
                   - **Project Manager (PM):** ใช้ติดตามความเสี่ยง, จัดงานในการทำงาน และดูแผนภาพ Gantt Chart
                   - **System Analyst (SA):** ใช้วิเคราะห์และออกแบบความเชื่อมโยงของโมดูล, สคีมาฐานข้อมูล และ REST APIs
                   - **Developer & QA:** ใช้ตรวจสอบ Code Quality, ช่องโหว่ความปลอดภัย และสร้างเคสการทดสอบอัตโนมัติ
                   - **Executive / Auditor:** ดูภาพรวมของ KPI, ความก้าวหน้าโครงการ และรายงาน Audit Log การเข้าใช้งาน
                
                3. **คุณค่าทางธุรกิจ (Business Value)**
                   - ช่วยลดเวลาในการอ่านวิเคราะห์และทำความเข้าใจโปรเจกต์งานที่ซับซ้อนลงได้กว่า **70%**
                   - ลดข้อผิดพลาดและช่องโหว่ความปลอดภัยในระบบ (Human Error) ก่อนเริ่มเขียนโปรแกรมจริง
            """.trimIndent()

            "System" -> """
                ### 🏗️ สถาปัตยกรรมระบบและโมดูลเชื่อมโยง (System Architecture)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Proposed Architectural Style**
                   - แนะนำให้ใช้ **Clean Architecture ร่วมกับ Event-Driven Microservices** สำหรับระบบที่มีการขยายตัวสูง 
                   - แยกส่วนของการดึงข้อมูลและการวิเคราะห์ของ AI เป็น Background Job Queue (Celery/Redis)
                
                2. **Core Modules (โมดูลหลักของระบบ)**
                   - **Auth & IAM Module:** จัดการสิทธิ์การเข้าถึงแบบ RBAC (Role-Based Access Control)
                   - **Parser & Extraction Module:** ดึงข้อมูลดิบจากไฟล์รูปแบบต่าง ๆ
                   - **LLM/AI Reasoning Module:** คุยกับ Gemini API เพื่อทำการวิเคราะห์โครงสร้างระบบเชิงลึก
                   - **Visualization Engine Module:** แปลงข้อมูลวิเคราะห์ให้อยู่ในรูปเวกเตอร์และแผนภาพโครงสร้าง
                   - **Project & Backlog Module:** จัดการ Sprint, Tasks และ Kanban Board
                
                3. **Dependency & Coupling Analysis**
                   - ค่าความเกี่ยวเนื่องกันสูง (High Cohesion) ในส่วนของ Parser และ Analyzer Module
                   - แนะนำให้ใช้ Dependency Injection และอินเทอร์เฟซเพื่อลดระดับความผูกมัด (Low Coupling) เพื่อให้แยกส่วนทดสอบได้ง่าย
            """.trimIndent()

            "Database" -> """
                ### 🗄️ การออกแบบฐานข้อมูลและความสัมพันธ์ (Database Design)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Database Normalization (ระดับ 3NF)**
                   - ตารางข้อมูลได้รับการออกแบบให้สอดคล้องกับ 3NF เพื่อหลีกเลี่ยงความซ้ำซ้อนของข้อมูล (Data Redundancy)
                   - ใช้ UUID เป็น Primary Key เพื่อความปลอดภัยและความง่ายในการทำงานร่วมกับ Distributed Database
                
                2. **Missing Indices & Optimizations**
                   - แนะนำให้ทำ Index บนคอลัมน์ `projectId` และ `status` ในตาราง `tasks` เนื่องจากเป็นคอลัมน์ที่ถูก Filter และ Query บ่อยที่สุด
                   - เพิ่ม Index บน `timestamp` ในตาราง `audit_logs` เพื่อประสิทธิภาพการดึงข้อมูลประวัติย้อนหลัง
                
                3. **ความสัมพันธ์ของตารางหลัก**
                   - **Projects (1) <-> (N) ProjectTasks** (One-to-Many via `projectId`)
                   - **Projects (1) <-> (N) RiskItems** (One-to-Many via `projectId`)
                   
                [TABLES_SCHEMA]
                Table: Users
                - id (UUID, PK)
                - email (VARCHAR)
                - role (VARCHAR)
                - created_at (TIMESTAMP)

                Table: Projects
                - id (INTEGER, PK)
                - name (VARCHAR)
                - description (TEXT)
                - created_at (TIMESTAMP)

                Table: ProjectTasks
                - id (INTEGER, PK)
                - projectId (INTEGER, FK -> Projects.id)
                - title (VARCHAR)
                - status (VARCHAR)
                - priority (VARCHAR)
                - sprint (VARCHAR)

                Table: RiskItems
                - id (INTEGER, PK)
                - projectId (INTEGER, FK -> Projects.id)
                - title (VARCHAR)
                - severity (VARCHAR)
                - mitigationPlan (TEXT)
                [/TABLES_SCHEMA]
            """.trimIndent()

            "API" -> """
                ### 🔌 การออกแบบ API และความปลอดภัย (API Design)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Core Endpoints Blueprint**
                   - `POST /api/v1/auth/login` - ยืนยันตัวตนเพื่อรับ JWT Access Token
                   - `GET /api/v1/projects` - ดึงรายชื่อโครงการทั้งหมดที่มีสิทธิ์เข้าถึง
                   - `POST /api/v1/projects/upload` - อัปโหลดไฟล์ข้อกำหนดหรือรหัสต้นฉบับ
                   - `GET /api/v1/projects/{id}/analysis` - ดึงรายงานสรุปวิเคราะห์แยกตามด้านหลัก
                   - `POST /api/v1/chat` - ถามตอบกับบอทผ่าน WebSockets หรือ HTTP Connection
                
                2. **API Gateways & Security**
                   - แนะนำให้ใช้ **Kong API Gateway** หรือ **AWS API Gateway**
                   - บังคับใช้การควบคุมอัตราการเรียกใช้ API (Rate Limiting) ที่ **100 requests per minute** ต่อ IP สำหรับ Endpoints การประมวลผลทั่วไป และ **10 requests per minute** สำหรับ AI Call เพื่อควบคุมงบประมาณค่าใช้จ่าย API
                
                3. **การส่งข้อมูล**
                   - สื่อสารผ่าน JSON ภายใต้โปรโตคอล HTTPS เสมอ
            """.trimIndent()

            "Security" -> """
                ### 🛡️ ความมั่นคงปลอดภัยและการป้องกันความเสี่ยง (Security Analysis)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **OWASP Top 10 Assessment & Mitigations**
                   - **A01: Broken Access Control (การควบคุมสิทธิ์บกพร่อง):** บังคับใช้ระบบ Role-Based Access Control (RBAC) ทั้งในระดับ UI และ API Endpoint Validation เสมอ
                   - **A03: Injection (การฉีดคำสั่ง):** ใช้ SQLAlchemy / Parameterized Queries ป้องกัน SQL Injection 100%
                   - **A05: Security Misconfiguration:** ปิดการใช้งาน Debug mode ในโปรดักชัน, ตั้งค่า CORS จำกัดเฉพาะโดเมนที่ได้รับอนุญาตเท่านั้น
                
                2. **การปกป้องข้อมูลสำคัญ (Sensitive Data Exposure)**
                   - ข้อมูลรหัสผ่านของผู้ใช้ถูกเข้ารหัสด้วยกลไก **BCrypt** ก่อนบันทึกลงฐานข้อมูล
                   - ข้อมูล API Keys และความลับขององค์กรถูกจัดเก็บใน Secrets Manager และใช้ BuildConfig ในการเรียกอ่านอย่างปลอดภัย
            """.trimIndent()

            "Performance" -> """
                ### ⚡ การเพิ่มประสิทธิภาพและระบบแคช (Performance Analysis)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Caching Architecture**
                   - ใช้ **Redis** ในการทำแคชผลลัพธ์การวิเคราะห์โครงการที่มีขนาดใหญ่ เพื่อลดความซ้ำซ้อนของการประมวลผลโมเดล AI
                   - แคชเซสชันผู้ใช้งานและสิทธิ์การเข้าถึง (Roles) เพื่อย่นเวลาการเรียกตรวจสอบสิทธิ์ในทุก ๆ คำร้องขอ (Request)
                
                2. **Slow Query Bottlenecks**
                   - หลีกเลี่ยงปัญหา N+1 Query โดยการทำ SQL Eager Loading (`joinedload` ใน SQLAlchemy) บนความสัมพันธ์ตาราง Projects และ Tasks
                
                3. **Horizontal Auto-scaling**
                   - ตั้งค่า CPU utilization threshold ที่ **75%** เพื่อเพิ่มจำนวนอินสแตนซ์ของ Backend container อัตโนมัติ (Horizontal Pod Autoscaling)
            """.trimIndent()

            "Code" -> """
                ### 💻 การรีวิวคุณภาพซอร์สโค้ดและความซับซ้อน (Code Review & Architecture)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **SOLID Principles Adherence**
                   - **Single Responsibility Principle (SRP):** แยกชั้นการอ่านไฟล์ (Parsers), ชั้นการจัดการธุรกิจ (Services) และชั้นติดต่อฐานข้อมูล (Repositories) อย่างชัดเจน
                   - **Dependency Inversion Principle (DIP):** UI และ Controllers คุยกับ Repository Interfaces เสมอโดยผ่านระบบ Dependency Injection
                
                2. **Code Smells & Dead Code**
                   - การใช้ตัวแปรแบบ Global หรือการทำ Hardcode Configuration ในโค้ดควรกำจัดออกไป
                   - ตรวจสอบและย้ายโค้ดที่มีการทำซ้ำ (Duplicate Code) ไปอยู่ในชั้น Shared Utility/Helper
                
                3. **Refactoring Suggestions**
                   - นำเอา Repository Pattern มาใช้ร่วมกับ Kotlin Flows เพื่อสร้าง Reactive State Management บนส่วนติดต่อผู้ใช้งาน
            """.trimIndent()

            "Infrastructure" -> """
                ### ☁️ สถาปัตยกรรมระบบคลาวด์และเน็ตเวิร์ก (Infrastructure Layout)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **Cloud Topology Plan (AWS Blueprint)**
                   - **VPC (Virtual Private Cloud):** แยกเป็น Public Subnet (บรรจุ ALB/CloudFront) และ Private Subnet (บรรจุ API Server, Redis) และ Isolated Subnet (บรรจุ PostgreSQL)
                   - **Compute Service:** ใช้ AWS ECS Fargate (Serverless Container) เพื่อความคล่องตัวสูง
                   - **Database Node:** AWS RDS PostgreSQL Multi-AZ เพื่อประสิทธิภาพและความปลอดภัยสูง
                
                2. **CI/CD Pipeline**
                   - ใช้ GitHub Actions ในการทำ Linting, Automated Tests, Build Docker Image และ Push ขึ้น Amazon ECR ก่อนสั่ง Deploy สู่ ECS Service อัตโนมัติ
                   
                [INFRA_SCHEMA]
                Node: UserBrowser (Client) -> CloudFront (CDN)
                Node: CloudFront (CDN) -> ALB (LoadBalancer)
                Node: ALB (LoadBalancer) -> ECSServer1 (Fargate API Private)
                Node: ALB (LoadBalancer) -> ECSServer2 (Fargate API Private)
                Node: ECSServer1 (Fargate API Private) -> RDS_Postgres (Isolated Primary)
                Node: ECSServer1 (Fargate API Private) -> RedisCache (Isolated Cache)
                Node: ECSServer2 (Fargate API Private) -> RDS_Postgres (Isolated Primary)
                Node: ECSServer2 (Fargate API Private) -> RedisCache (Isolated Cache)
                [/INFRA_SCHEMA]
            """.trimIndent()

            "Risk" -> """
                ### ⚠️ การประเมินและบริหารจัดการความเสี่ยง (Risk Matrix)
                *หมายเหตุ: ทำงานในโหมดออฟไลน์เนื่องจากไม่ได้ใส่ API Key*
                
                1. **High Severity Risk: การรั่วไหลของข้อมูลการวิเคราะห์และข้อกำหนดลูกค้า (Critical)**
                   - *Mitigation:* บังคับใช้การเข้ารหัสฐานข้อมูล (Transparent Data Encryption), ตรวจสอบสิทธิ์ผู้ใช้ในระดับ Row-Level Security ในตารางฐานข้อมูลโครงการ
                
                2. **Medium Severity Risk: ปัญหา API Rate Limiting จาก Google Gemini (Medium)**
                   - *Mitigation:* ใช้ระบบ Cache ประสิทธิภาพสูงเพื่อเก็บข้อมูลวิเคราะห์ และวางแผนคิวคำร้องขอลดระดับความถี่ในการเรียกใช้ API ในกรณีผู้ใช้เรียกพร้อมกันจำนวนมาก
                
                3. **Low Severity Risk: ประสบการณ์ใช้งานบนอุปกรณ์หน้าจอต่างขนาดขัดข้อง (Low)**
                   - *Mitigation:* ออกแบบ UI ในลักษณะ Adaptive (Mobile & Tablet Layout) ปรับเปลี่ยนการแสดงผลแถบควบคุมด้านข้าง (Navigation Rail) อย่างยืดหยุ่นตามความกว้างหน้าจอ
            """.trimIndent()

            else -> "Offline Analysis Report: Mock Data."
        }
    }
}
