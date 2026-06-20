package top.cuteruarua.hooksms.sender

import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge
import org.json.JSONObject
import top.cuteruarua.hooksms.config.SenderConfigStore
import top.cuteruarua.hooksms.message.MessageFormatter

class FeishuBotSender : HttpSender(), ConfigurableSender {

    companion object {
        private const val KEY_WEBHOOK_URL = "webhook_url"
    }

    override val configName: String = "feishu_bot"

    override fun send(lpparam: XC_LoadPackage.LoadPackageParam, data: SmsData): SendResult {
        return try {
            val config = loadConfig(lpparam)
                ?: return SendResult(false, "Config missing", "Please configure Feishu Bot first")

            val message = buildMessage(data)
            XposedBridge.log("HookSMS: Send to Feishu Bot - ${config.webhookUrl}")

            val response = executeJsonRequest(buildRequest(config.webhookUrl, message))
            if (isSuccessResponse(response)) {
                SendResult(true, "Send success", response.toString())
            } else {
                val errorMsg = response.optString("msg")
                    .ifBlank { response.optString("message") }
                    .ifBlank { response.toString() }
                SendResult(false, "Send failed", errorMsg)
            }
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: Feishu Bot send failed - ${e.message}")
            SendResult(false, "Send error", e.message)
        }
    }

    override fun buildMessage(data: SmsData): String {
        return MessageFormatter.formatSmsMessage(data)
    }

    override fun getConfigFields(): List<ConfigFieldSpec> {
        return listOf(
            ConfigFieldSpec(
                key = KEY_WEBHOOK_URL,
                label = "Webhook URL",
                type = ConfigFieldType.TEXT,
                hint = "https://open.feishu.cn/open-apis/bot/v2/hook/..."
            )
        )
    }

    override fun validateConfigValues(values: Map<String, String>): String? {
        val webhookUrl = values[KEY_WEBHOOK_URL].orEmpty().trim()
        return if (webhookUrl.startsWith("http://") || webhookUrl.startsWith("https://")) {
            null
        } else {
            "Please enter a valid Feishu webhook URL"
        }
    }

    override fun testConnection(values: Map<String, String>): ConfigTestResult {
        val webhookUrl = values[KEY_WEBHOOK_URL].orEmpty().trim()
        val response = executeRequest(
            buildRequest(
                webhookUrl = webhookUrl,
                message = MessageFormatter.formatTestMessage(0)
            )
        )
        val detail = formatHttpResponse(response)
        val jsonResult = runCatching { response.bodyAsJson() }
        val success = response.isSuccessful &&
            (jsonResult.getOrNull()?.let { isSuccessResponse(it) } == true)
        return ConfigTestResult(success, detail)
    }

    override fun isSuccessResponse(response: JSONObject): Boolean {
        return response.optInt("code", -1) == 0
    }

    private fun loadConfig(lpparam: XC_LoadPackage.LoadPackageParam): Config? {
        return try {
            val values = SenderConfigStore.loadForHook(this)
            val webhookUrl = values[KEY_WEBHOOK_URL]?.trim()?.takeIf { it.isNotBlank() }
            if (webhookUrl != null) Config(webhookUrl) else null
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: load Feishu config failed - ${e.message}")
            null
        }
    }

    private fun buildRequest(webhookUrl: String, message: String): HttpRequestSpec {
        val body = JSONObject().apply {
            put("msg_type", "text")
            put(
                "content",
                JSONObject().apply {
                    put("text", message)
                }
            )
        }.toString()

        return HttpRequestSpec(
            url = webhookUrl,
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
    }

    data class Config(
        val webhookUrl: String
    )
}
