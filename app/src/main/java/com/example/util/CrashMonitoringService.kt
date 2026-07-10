package com.example.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
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

    @Volatile
    private var isAppInForeground = false

    init {
        setupForegroundTracking()
        loadStoredCrashes()
        setupUncaughtExceptionHandler()
        startPeriodicPerformanceMonitoring()
    }

    private fun setupForegroundTracking() {
        val app = context.applicationContext as? Application
        app?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                isAppInForeground = startedActivities > 0
                Log.d(TAG, "Activity started: $activity, App in foreground: $isAppInForeground")
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                isAppInForeground = startedActivities > 0
                Log.d(TAG, "Activity stopped: $activity, App in foreground: $isAppInForeground")
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
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
        try {
            val jsonArray = JSONArray()
            for (crash in list) {
                val obj = JSONObject().apply {
                    put("timestamp", crash.timestamp)
                    put("exceptionName", crash.exceptionName)
                    put("errorMessage", crash.errorMessage)
                    put("stackTrace", crash.stackTrace)
                    put("deviceModel", crash.deviceModel)
                    put("androidVersion", crash.androidVersion)
                }
                jsonArray.put(obj)
            }
            sp.edit().putString(KEY_CRASHES, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize crash logs to JSON", e)
        }
    }

    private fun loadStoredCrashes() {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serialized = sp.getString(KEY_CRASHES, null) ?: return
        try {
            val jsonArray = JSONArray(serialized)
            val list = mutableListOf<CrashLog>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CrashLog(
                        timestamp = obj.getLong("timestamp"),
                        exceptionName = obj.getString("exceptionName"),
                        errorMessage = obj.getString("errorMessage"),
                        stackTrace = obj.getString("stackTrace"),
                        deviceModel = obj.getString("deviceModel"),
                        androidVersion = obj.getString("androidVersion")
                    )
                )
            }
            _crashes.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading crash histories via JSON: ", e)
        }
    }

    private fun startPeriodicPerformanceMonitoring() {
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    if (isAppInForeground) {
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
                    }
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
