package top.cuteruarua.hooksms.sender

import android.content.Context
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import top.cuteruarua.hooksms.message.MessageFormatter

enum class HttpMethod {
    GET,
    POST
}

data class HttpRequestSpec(
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
)

data class HttpResponse(
    val code: Int,
    val body: String
) {
    val isSuccessful: Boolean
        get() = code in 200..299

    fun bodyAsJson(): JSONObject = JSONObject(body)
}

abstract class HttpSender : SmsSender {

    companion object {
        const val CONNECT_TIMEOUT = 10000
        const val READ_TIMEOUT = 10000
        private const val MODULE_PACKAGE = "top.cuteruarua.hooksms"
    }

    protected fun createModuleContext(lpparam: XC_LoadPackage.LoadPackageParam): Context? {
        return try {
            val currentContext = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentApplication"
            ) as? Context

            if (currentContext != null) {
                XposedHelpers.callMethod(
                    currentContext,
                    "createPackageContext",
                    MODULE_PACKAGE,
                    0
                ) as? Context
            } else {
                null
            }
        } catch (e: Exception) {
            logToXposed("HookSMS: createModuleContext failed - ${e.message}")
            null
        }
    }

    protected fun executeRequest(spec: HttpRequestSpec): HttpResponse {
        val connection = URL(spec.url).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = spec.method.name
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT

            spec.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            spec.body?.let { body ->
                connection.doOutput = true
                connection.outputStream.use { outputStream ->
                    outputStream.write(body.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val response = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            logToXposed("HookSMS: HTTP response code: $responseCode")
            logToXposed("HookSMS: HTTP response body: $response")

            return HttpResponse(responseCode, response)
        } finally {
            connection.disconnect()
        }
    }

    protected fun executeJsonRequest(spec: HttpRequestSpec): JSONObject {
        return executeRequest(spec).bodyAsJson()
    }

    protected fun formatHttpResponse(response: HttpResponse): String {
        return "HTTP ${response.code}\n${response.body}"
    }

    protected open fun buildMessage(data: SmsData): String {
        return MessageFormatter.formatSmsMessage(data)
    }

    protected open fun isSuccessResponse(response: JSONObject): Boolean {
        return response.optString("status") == "ok" ||
            response.optInt("retcode", -1) == 0 ||
            response.optInt("code", -1) == 0
    }

    private fun logToXposed(message: String) {
        try {
            val bridge = Class.forName("de.robv.android.xposed.XposedBridge")
            val logMethod = bridge.getMethod("log", String::class.java)
            logMethod.invoke(null, message)
        } catch (_: Throwable) {
            // ConfigActivity tests run without Xposed classes on the app classpath.
        }
    }
}
