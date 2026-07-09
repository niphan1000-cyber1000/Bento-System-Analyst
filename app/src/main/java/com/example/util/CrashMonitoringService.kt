package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class CrashLog(
    val timestamp: Long,
    val exceptionName: String,
    val errorMessage: String,
    val stackTrace: String,
    val deviceModel: String,
    val androidVersion: String
)

data class PerformanceMetrics(
    val usedMemoryMb: Long,
    val totalMemoryMb: Long,
    val maxMemoryMb: Long,
    val activeThreads: Int,
    val networkLatencyMs: Long
)

class CrashMonitoringService(private val context: Context) {
    private val TAG = "CrashMonitoring"
    private val PREFS_NAME = "crash_monitoring_prefs"
    private val KEY_CRASHES = "saved_crashes"

    private val _crashes = MutableStateFlow<List<CrashLog>>(emptyList())
    val crashes: StateFlow<List<CrashLog>> = _crashes.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(
        PerformanceMetrics(
            usedMemoryMb = 0,
            totalMemoryMb = 0,
            maxMemoryMb = 0,
            activeThreads = 0,
            networkLatencyMs = -1
        )
    )
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    init {
        loadStoredCrashes()
        setupUncaughtExceptionHandler()
        startPeriodicPerformanceMonitoring()
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Capture crash
            logCrash(throwable)
            
            // Allow default android crash behavior after capturing
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logCrash(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceStr = sw.toString()

        val crash = CrashLog(
            timestamp = System.currentTimeMillis(),
            exceptionName = throwable.javaClass.simpleName,
            errorMessage = throwable.message ?: "Unknown error",
            stackTrace = stackTraceStr,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "SDK ${Build.VERSION.SDK_INT}"
        )

        Log.e(TAG, "Uncaught Exception Intercepted: ${crash.exceptionName} - ${crash.errorMessage}")
        
        // Append to local shared preferences
        val updated = _crashes.value.toMutableList().apply {
            add(0, crash)
        }
        _crashes.value = updated
        saveCrashesToPrefs(updated)
    }

    fun clearCrashes() {
        _crashes.value = emptyList()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CRASHES)
            .apply()
    }

    private fun saveCrashesToPrefs(list: List<CrashLog>) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serialized = list.joinToString("##CRASH##") { crash ->
            "${crash.timestamp}|${crash.exceptionName}|${crash.errorMessage}|${crash.stackTrace.replace("\n", "##LINE##")}|${crash.deviceModel}|${crash.androidVersion}"
        }
        sp.edit().putString(KEY_CRASHES, serialized).apply()
    }

    private fun loadStoredCrashes() {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serialized = sp.getString(KEY_CRASHES, null) ?: return
        try {
            val list = serialized.split("##CRASH##")
                .filter { it.isNotEmpty() }
                .map { item ->
                    val parts = item.split("|")
                    CrashLog(
                        timestamp = parts[0].toLong(),
                        exceptionName = parts[1],
                        errorMessage = parts[2],
                        stackTrace = parts[3].replace("##LINE##", "\n"),
                        deviceModel = parts[4],
                        androidVersion = parts[5]
                    )
                }
            _crashes.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading crash histories: ", e)
        }
    }

    private fun startPeriodicPerformanceMonitoring() {
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    val runtime = Runtime.getRuntime()
                    val maxMem = runtime.maxMemory() / (1024 * 1024)
                    val totalMem = runtime.totalMemory() / (1024 * 1024)
                    val freeMem = runtime.freeMemory() / (1024 * 1024)
                    val usedMem = totalMem - freeMem

                    val activeCount = Thread.activeCount()
                    val ping = testGatewayLatency()

                    _performanceMetrics.value = PerformanceMetrics(
                        usedMemoryMb = usedMem,
                        totalMemoryMb = totalMem,
                        maxMemoryMb = maxMem,
                        activeThreads = activeCount,
                        networkLatencyMs = ping
                    )
                    
                    Thread.sleep(3000) // update every 3 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error during metrics monitor cycle", e)
                    try { Thread.sleep(5000) } catch (ignored: Exception) {}
                }
            }
        }
    }

    private fun testGatewayLatency(): Long {
        val start = System.currentTimeMillis()
        return try {
            val url = URL("https://generativelanguage.googleapis.com/")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            connection.requestMethod = "GET"
            connection.connect()
            connection.disconnect()
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1L // Offline or unreachable
        }
    }
}
