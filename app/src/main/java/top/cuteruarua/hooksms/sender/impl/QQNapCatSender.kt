package top.cuteruarua.hooksms.sender

import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge
import org.json.JSONObject
import top.cuteruarua.hooksms.config.SenderConfigStore
import top.cuteruarua.hooksms.message.MessageFormatter

class QQNapCatSender : HttpSender(), ConfigurableSender {

    companion object {
        private const val KEY_SERVER = "server"
        private const val KEY_TOKEN = "token"
        private const val KEY_GROUP_ID = "group_id"
    }

    override val configName: String = "qq_napcat"

    override fun send(lpparam: XC_LoadPackage.LoadPackageParam, data: SmsData): SendResult {
        return try {
            val config = loadConfig(lpparam)
                ?: return SendResult(false, "Config missing", "Please configure QQ NapCat first")

            val message = buildMessage(data)
            val request = buildRequest(config.server, config.token, config.groupId, message)

            XposedBridge.log("HookSMS: Send to QQ group ${config.groupId}")

            val response = executeJsonRequest(request)

            if (isSuccessResponse(response)) {
                val messageId = response.optJSONObject("data")?.optLong("message_id")
                SendResult(true, "Send success", "message_id=$messageId")
            } else {
                val errorMsg = response.optString("message").ifBlank { response.optString("wording") }
                SendResult(false, "Send failed", errorMsg)
            }
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: QQ NapCat send failed - ${e.message}")
            SendResult(false, "Send error", e.message)
        }
    }

    fun testConnection(server: String, token: String, groupId: Long): ConfigTestResult {
        val testMessage = MessageFormatter.formatTestMessage(groupId)
        val response = executeRequest(buildRequest(server, token, groupId, testMessage))
        val jsonResult = runCatching { response.bodyAsJson() }
        val success = response.isSuccessful &&
            (jsonResult.getOrNull()?.let { isSuccessResponse(it) } == true)
        val detail = buildString {
            append(formatHttpResponse(response))
            jsonResult.exceptionOrNull()?.let {
                append("\nJSON parse error: ${it.message}")
            }
        }
        return ConfigTestResult(success, detail)
    }

    override fun getConfigFields(): List<ConfigFieldSpec> {
        return listOf(
            ConfigFieldSpec(
                key = KEY_SERVER,
                label = "Server (IP:PORT)",
                type = ConfigFieldType.TEXT,
                hint = "Server address"
            ),
            ConfigFieldSpec(
                key = KEY_TOKEN,
                label = "Token",
                type = ConfigFieldType.PASSWORD,
                hint = "Token"
            ),
            ConfigFieldSpec(
                key = KEY_GROUP_ID,
                label = "Group ID",
                type = ConfigFieldType.NUMBER,
                hint = "QQ group id"
            )
        )
    }

    override fun validateConfigValues(values: Map<String, String>): String? {
        val server = values[KEY_SERVER].orEmpty()
        val token = values[KEY_TOKEN].orEmpty()
        val groupId = values[KEY_GROUP_ID]?.toLongOrNull() ?: 0L
        return if (server.isBlank() || token.isBlank() || groupId <= 0L) {
            "Please complete QQ config"
        } else {
            null
        }
    }

    override fun testConnection(values: Map<String, String>): ConfigTestResult {
        return testConnection(
            server = values[KEY_SERVER].orEmpty(),
            token = values[KEY_TOKEN].orEmpty(),
            groupId = values[KEY_GROUP_ID]?.toLongOrNull() ?: 0L
        )
    }

    private fun buildPayload(groupId: Long, message: String): JSONObject {
        return JSONObject().apply {
            put("group_id", groupId)
            put("message", message)
        }
    }

    private fun buildRequest(
        server: String,
        token: String,
        groupId: Long,
        message: String
    ): HttpRequestSpec {
        return HttpRequestSpec(
            url = "http://$server/send_group_msg",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = buildPayload(groupId, message).toString()
        )
    }

    private fun loadConfig(lpparam: XC_LoadPackage.LoadPackageParam): Config? {
        return try {
            val values = SenderConfigStore.loadForHook(this)
            val server = values[KEY_SERVER]?.takeIf { it.isNotBlank() }
            val token = values[KEY_TOKEN]?.takeIf { it.isNotBlank() }
            val groupId = values[KEY_GROUP_ID]?.toLongOrNull() ?: 0L

            if (server != null && token != null && groupId > 0L) {
                Config(server, token, groupId)
            } else {
                null
            }
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: load QQ config failed - ${e.message}")
            null
        }
    }

    data class Config(
        val server: String,
        val token: String,
        val groupId: Long
    )
}
