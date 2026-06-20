package top.cuteruarua.hooksms.sender

import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge
import org.json.JSONObject
import top.cuteruarua.hooksms.config.SenderConfigStore
import top.cuteruarua.hooksms.message.MessageFormatter

class CustomHttpSender : HttpSender(), ConfigurableSender {

    companion object {
        private const val KEY_URL = "url"
        private const val KEY_METHOD = "method"
        private const val KEY_HEADERS = "headers"
        private const val KEY_DATA_TEMPLATE = "data_template"
    }

    override val configName: String = "custom_http"

    override fun send(lpparam: XC_LoadPackage.LoadPackageParam, data: SmsData): SendResult {
        return try {
            val config = loadConfig(lpparam)
                ?: return SendResult(false, "Config missing", "Please configure Custom HTTP first")

            val message = buildMessage(data)
            XposedBridge.log("HookSMS: Send to Custom HTTP - ${config.url}")

            val response = executeJsonRequest(buildRequest(config, message))

            if (isSuccessResponse(response)) {
                SendResult(true, "Send success", "response=${response}")
            } else {
                val errorMsg = response.optString("message")
                    .ifBlank { response.optString("error") }
                    .ifBlank { response.toString() }
                SendResult(false, "Send failed", errorMsg)
            }
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: Custom HTTP send failed - ${e.message}")
            SendResult(false, "Send error", e.message)
        }
    }

    override fun buildMessage(data: SmsData): String {
        return MessageFormatter.formatSmsMessage(data)
    }

    fun testConnection(
        url: String,
        method: HttpMethod,
        headersStr: String,
        dataTemplate: String
    ): ConfigTestResult {
        val testMessage = MessageFormatter.formatTestMessage(0)
        val headers = parseHeaders(headersStr)
        val request = buildRequest(
            Config(
                url = url,
                method = method,
                headers = headers,
                dataTemplate = dataTemplate
            ),
            testMessage
        )

        val response = executeRequest(
            request
        )
        val detail = buildString {
            append(formatRequestPreview(request))
            append("\nResponse:\n")
            append(formatHttpResponse(response))
        }
        return ConfigTestResult(response.isSuccessful, detail)
    }

    override fun getConfigFields(): List<ConfigFieldSpec> {
        return listOf(
            ConfigFieldSpec(
                key = KEY_URL,
                label = "Request URL",
                type = ConfigFieldType.TEXT,
                hint = "http://example.com/api"
            ),
            ConfigFieldSpec(
                key = KEY_METHOD,
                label = "HTTP Method",
                type = ConfigFieldType.SELECT,
                options = HttpMethod.entries.map { ConfigFieldOption(it.name, it.name) }
            ),
            ConfigFieldSpec(
                key = KEY_HEADERS,
                label = "Headers (Key:Value;Key2:Value2)",
                type = ConfigFieldType.TEXT,
                hint = "Content-Type:application/json;Authorization:Bearer xxx"
            ),
            ConfigFieldSpec(
                key = KEY_DATA_TEMPLATE,
                label = "Data Template (\$text is message content)",
                type = ConfigFieldType.MULTILINE,
                hintProvider = { values ->
                    when (values[KEY_METHOD] ?: HttpMethod.POST.name) {
                        HttpMethod.GET.name -> "?msg=\$text&from=sms"
                        else -> "{\"text\":\"\$text\",\"from\":\"sms\"}"
                    }
                }
            )
        )
    }

    override fun validateConfigValues(values: Map<String, String>): String? {
        val url = values[KEY_URL].orEmpty()
        val headers = values[KEY_HEADERS].orEmpty()
        val dataTemplate = values[KEY_DATA_TEMPLATE].orEmpty()
        return if (url.isBlank() || headers.isBlank() || dataTemplate.isBlank()) {
            "Please complete Custom HTTP config"
        } else {
            null
        }
    }

    override fun testConnection(values: Map<String, String>): ConfigTestResult {
        val method = runCatching {
            HttpMethod.valueOf(values[KEY_METHOD] ?: HttpMethod.POST.name)
        }.getOrDefault(HttpMethod.POST)

        return testConnection(
            url = values[KEY_URL].orEmpty(),
            method = method,
            headersStr = values[KEY_HEADERS].orEmpty(),
            dataTemplate = values[KEY_DATA_TEMPLATE].orEmpty()
        )
    }

    private fun loadConfig(lpparam: XC_LoadPackage.LoadPackageParam): Config? {
        return try {
            val values = SenderConfigStore.loadForHook(this)
            val url = values[KEY_URL]?.takeIf { it.isNotBlank() }
            val methodStr = values[KEY_METHOD].orEmpty().ifBlank { HttpMethod.POST.name }
            val headersStr = values[KEY_HEADERS]?.takeIf { it.isNotBlank() }
            val dataTemplate = values[KEY_DATA_TEMPLATE]?.takeIf { it.isNotBlank() }

            if (url != null && headersStr != null && dataTemplate != null) {
                Config(
                    url = url,
                    method = HttpMethod.valueOf(methodStr),
                    headers = parseHeaders(headersStr),
                    dataTemplate = dataTemplate
                )
            } else {
                null
            }
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: load Custom HTTP config failed - ${e.message}")
            null
        }
    }

    private fun parseHeaders(headersStr: String): Map<String, String> {
        return try {
            headersStr.split(";")
                .mapNotNull {
                    val parts = it.split(":", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                }
                .toMap()
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: parse headers failed - ${e.message}")
            emptyMap()
        }
    }

    private fun buildRequest(config: Config, message: String): HttpRequestSpec {
        return when (config.method) {
            HttpMethod.GET -> {
                val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                val query = config.dataTemplate.replace("\$text", encodedMessage)
                HttpRequestSpec(
                    url = config.url + query,
                    method = HttpMethod.GET,
                    headers = config.headers
                )
            }

            HttpMethod.POST -> {
                val body = config.dataTemplate.replace(
                    "\$text",
                    buildTemplateValue(config.headers, message)
                )
                HttpRequestSpec(
                    url = config.url,
                    method = HttpMethod.POST,
                    headers = config.headers,
                    body = body
                )
            }
        }
    }

    internal fun previewRequest(
        method: HttpMethod,
        url: String,
        headers: Map<String, String>,
        dataTemplate: String,
        message: String
    ): HttpRequestSpec {
        return buildRequest(
            Config(
                url = url,
                method = method,
                headers = headers,
                dataTemplate = dataTemplate
            ),
            message
        )
    }

    private fun buildTemplateValue(headers: Map<String, String>, message: String): String {
        return if (isJsonContentType(headers)) {
            escapeJsonString(message)
        } else {
            message
        }
    }

    private fun isJsonContentType(headers: Map<String, String>): Boolean {
        return headers.entries.any { (key, value) ->
            key.equals("Content-Type", ignoreCase = true) &&
                value.contains("application/json", ignoreCase = true)
        }
    }

    private fun escapeJsonString(value: String): String {
        return JSONObject.quote(value)
            .removePrefix("\"")
            .removeSuffix("\"")
    }

    private fun formatRequestPreview(spec: HttpRequestSpec): String {
        return buildString {
            append("Request:\n")
            append("URL: ${spec.url}\n")
            append("Method: ${spec.method}\n")
            append("Headers:\n")
            if (spec.headers.isEmpty()) {
                append("(none)\n")
            } else {
                spec.headers.forEach { (key, value) ->
                    append("$key: $value\n")
                }
            }
            append("Data:\n")
            append(spec.body ?: "(none)")
        }
    }

    data class Config(
        val url: String,
        val method: HttpMethod,
        val headers: Map<String, String>,
        val dataTemplate: String
    )
}
