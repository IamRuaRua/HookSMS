package top.cuteruarua.hooksms

import android.content.Context
import de.robv.android.xposed.XposedBridge

object LogManager {

    private const val LOGS_PREFS = "hooksms_logs"
    private const val KEY_LOGS = "logs"
    private const val MAX_LOG_SIZE = 50000
    private const val MODULE_PACKAGE = "top.cuteruarua.hooksms"

    fun log(message: String, context: Context? = null) {
        logToXposed("HookSMS: $message")
        context?.takeIf { shouldPersistLogs(it) }?.let { saveLog(message, it) }
    }

    fun clearLogs(context: Context) {
        val prefs = context.getSharedPreferences(LOGS_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOGS, "").apply()
        logToXposed("HookSMS: Logs cleared")
    }

    fun getLogs(context: Context): String {
        val prefs = context.getSharedPreferences(LOGS_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGS, "") ?: ""
    }

    private fun saveLog(message: String, context: Context) {
        val prefs = context.getSharedPreferences(LOGS_PREFS, Context.MODE_PRIVATE)
        val currentLogs = prefs.getString(KEY_LOGS, "") ?: ""
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message\n"

        var newLogs = currentLogs + logEntry
        if (newLogs.length > MAX_LOG_SIZE) {
            newLogs = newLogs.substring(newLogs.length - MAX_LOG_SIZE)
        }

        prefs.edit().putString(KEY_LOGS, newLogs).apply()
    }

    private fun shouldPersistLogs(context: Context): Boolean {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getMethod("currentApplication").invoke(null) as? Context
            currentApp?.packageName == MODULE_PACKAGE && context.packageName == MODULE_PACKAGE
        } catch (_: Throwable) {
            context.packageName == MODULE_PACKAGE
        }
    }

    private fun logToXposed(message: String) {
        try {
            XposedBridge.log(message)
        } catch (_: Throwable) {
            // The standalone config Activity runs without the Xposed API on its classpath.
        }
    }
}
