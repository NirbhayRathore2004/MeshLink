package com.meshlink.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableMessage: String? = null
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

object MeshLogger {
    const val TAG_MESH = "Mesh"
    const val TAG_TRANSPORT = "Transport"
    const val TAG_ROUTING = "Routing"
    const val TAG_FORWARDING = "Forwarding"
    const val TAG_DELIVERY = "Delivery"
    const val TAG_ACK = "ACK"
    const val TAG_DATABASE = "Database"
    const val TAG_SERVICE = "Service"

    private const val MAX_LOG_ENTRIES = 500

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun d(tag: String, message: String) {
        try { Log.d(tag, message) } catch (_: Throwable) {}
        appendLog(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        try { Log.i(tag, message) } catch (_: Throwable) {}
        appendLog(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try { Log.w(tag, message, throwable) } catch (_: Throwable) {}
        appendLog(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try { Log.e(tag, message, throwable) } catch (_: Throwable) {}
        appendLog(LogLevel.ERROR, tag, message, throwable)
    }

    private fun appendLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwableMessage = throwable?.message
        )
        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_LOG_ENTRIES) {
                updated.takeLast(MAX_LOG_ENTRIES)
            } else {
                updated
            }
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
