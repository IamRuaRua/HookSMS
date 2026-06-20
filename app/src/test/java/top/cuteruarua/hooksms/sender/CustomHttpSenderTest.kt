package top.cuteruarua.hooksms.sender

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomHttpSenderTest {

    @Test
    fun `post json escapes text placeholder`() {
        val sender = CustomHttpSender()

        val request = sender.previewRequest(
            method = HttpMethod.POST,
            url = "http://127.0.0.1:3000/send_group_msg",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer abc123def456"
            ),
            dataTemplate = """{"group_id":987654321,"message":"${'$'}text"}""",
            message = "From: 10086\nMessage:\nhello \"world\"\\done"
        )

        assertEquals(
            """{"group_id":987654321,"message":"From: 10086\nMessage:\nhello \"world\"\\done"}""",
            request.body
        )
    }

    @Test
    fun `post non json keeps raw text placeholder`() {
        val sender = CustomHttpSender()

        val request = sender.previewRequest(
            method = HttpMethod.POST,
            url = "http://127.0.0.1:3000/send",
            headers = mapOf("Content-Type" to "text/plain"),
            dataTemplate = "payload=${'$'}text",
            message = "line1\nline2"
        )

        assertEquals("payload=line1\nline2", request.body)
    }
}
