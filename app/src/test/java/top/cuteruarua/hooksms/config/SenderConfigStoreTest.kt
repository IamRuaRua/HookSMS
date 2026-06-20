package top.cuteruarua.hooksms.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.cuteruarua.hooksms.sender.FeishuBotSender
import top.cuteruarua.hooksms.sender.ConfigFieldSpec
import top.cuteruarua.hooksms.sender.ConfigFieldType
import top.cuteruarua.hooksms.sender.ConfigTestResult
import top.cuteruarua.hooksms.sender.ConfigurableSender

class SenderConfigStoreTest {

    @Test
    fun `buildStoredValues keeps only declared sender fields and fills blanks`() {
        val sender = object : ConfigurableSender {
            override val configName: String = "fake_sender"

            override fun getConfigFields(): List<ConfigFieldSpec> {
                return listOf(
                    ConfigFieldSpec("url", "URL", ConfigFieldType.TEXT),
                    ConfigFieldSpec("token", "Token", ConfigFieldType.PASSWORD)
                )
            }

            override fun validateConfigValues(values: Map<String, String>): String? = null

            override fun testConnection(values: Map<String, String>): ConfigTestResult {
                return ConfigTestResult(success = true, detail = "ok")
            }
        }

        val stored = SenderConfigStore.buildStoredValues(
            sender = sender,
            values = mapOf(
                "url" to "http://localhost",
                "extra" to "ignored"
            )
        )

        assertEquals(
            mapOf(
                "url" to "http://localhost",
                "token" to ""
            ),
            stored
        )
        assertTrue("extra" !in stored)
    }

    @Test
    fun `feishu sender exposes single webhook field`() {
        val fields = FeishuBotSender().getConfigFields()

        assertEquals(1, fields.size)
        assertEquals("webhook_url", fields.single().key)
        assertEquals(ConfigFieldType.TEXT, fields.single().type)
    }
}
