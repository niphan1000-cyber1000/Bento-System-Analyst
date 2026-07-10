package com.aistudio.aisystemanalyst.network

import android.util.Log
import com.aistudio.aisystemanalyst.BuildConfig
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

    @Volatile
    var enterpriseGatewayToken: String = ""

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
            
            val requestBuilder = Request.Builder()
                .url(enterpriseGatewayUrl)
                .post(requestBody)

            if (enterpriseGatewayToken.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $enterpriseGatewayToken")
            } else {
                // Attach safe default mock token for sandbox demonstration if empty
                requestBuilder.addHeader("Authorization", "Bearer enterpriseMockToken_NotConfigured")
            }
            
            val request = requestBuilder.build()

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
            Log.e(TAG, "Gateway analysis call failed", e)
            throw Exception("Enterprise Gateway Call Failed: ไม่สามารถเชื่อมต่อกับ Enterprise Secure Proxy ($enterpriseGatewayUrl) ได้ในขณะนี้\n\nรายละเอียดความล้มเหลว: ${e.localizedMessage ?: "Connection Timeout"}\n\n*ระบบความปลอดภัยแบบ Zero-Trust ถูกเปิดใช้งาน: ไม่ทำการ Fallback ไปยิง API โดยตรงจากเครื่อง Client เพื่อป้องกันข้อมูลของแอปพลิเคชันรั่วไหล*")
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
            throw Exception("ไม่พบคีย์การเข้าถึงบริการวิเคราะห์: กรุณาตั้งค่าคีย์ API 'GEMINI_API_KEY' ในแผง Secrets ของ AI Studio หรือไฟล์คอนฟิกูเรชัน .env ให้ถูกต้องเพื่อวิเคราะห์ข้อมูลจริง หรือเปิดใช้งานโหมด Enterprise Secure Gateway ด้านบนแทน")
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

}
