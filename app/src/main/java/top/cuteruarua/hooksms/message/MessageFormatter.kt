package top.cuteruarua.hooksms.message

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.cuteruarua.hooksms.sender.SmsData

object MessageFormatter {

    fun formatSmsMessage(data: SmsData): String {
        return """
            From: ${data.originatingAddress ?: "unknown"}
            Message:
            ${data.messageBody ?: ""}
            Time: ${data.timestamp}
        """.trimIndent()
    }

    fun formatSimple(data: SmsData): String {
        return "SMS ${data.originatingAddress ?: "unknown"}: ${data.messageBody ?: ""}"
    }

    fun formatDetailed(data: SmsData): String {
        val simInfo = data.simSlot?.let { "\nSIM: $it" }.orEmpty()
        val recipientInfo = data.recipientNumber?.let { "\nRecipient: $it" }.orEmpty()

        return """
            SMS Notification
            ----------------
            From: ${data.originatingAddress ?: "unknown"}
            Message: ${data.messageBody ?: ""}
            Time: ${data.timestamp}$simInfo$recipientInfo
        """.trimIndent()
    }

    fun formatTestMessage(groupId: Long): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        return """
            HookSMS test message
            Group: $groupId
            Time: $timestamp
        """.trimIndent()
    }

    enum class MessageType(val displayName: String) {
        DEFAULT("Default"),
        SIMPLE("Simple"),
        DETAILED("Detailed"),
        CUSTOM("Custom")
    }

    fun format(data: SmsData, type: MessageType = MessageType.DEFAULT): String {
        return when (type) {
            MessageType.DEFAULT -> formatSmsMessage(data)
            MessageType.SIMPLE -> formatSimple(data)
            MessageType.DETAILED -> formatDetailed(data)
            MessageType.CUSTOM -> formatSmsMessage(data)
        }
    }

    fun formatCustom(data: SmsData, template: String): String {
        return template
            .replace("{from}", data.originatingAddress ?: "unknown")
            .replace("{content}", data.messageBody ?: "")
            .replace("{time}", data.timestamp)
            .replace("{sim}", data.simSlot ?: "unknown")
            .replace("{recipient}", data.recipientNumber ?: "unknown")
    }

    object Templates {
        const val TEMPLATE_DEFAULT = "SMS from {from}\n{content}\nTime: {time}"
        const val TEMPLATE_COMPACT = "[SMS] {from}: {content}"
        const val TEMPLATE_TECHNICAL = "[HookSMS]\nFrom: {from}\nContent: {content}\nTime: {time}\nSIM: {sim}"
        const val TEMPLATE_FRIENDLY = "New SMS from {from}\n\n{content}\n\nTime: {time}"
    }
}
