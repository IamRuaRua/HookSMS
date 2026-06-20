package top.cuteruarua.hooksms.sender

data class SenderDefinition(
    val type: SenderType,
    val sender: SmsSender
)

object SenderRegistry {

    private val definitions = listOf(
        SenderDefinition(SenderType.QQ_NAPCAT, QQNapCatSender()),
        SenderDefinition(SenderType.FEISHU_BOT, FeishuBotSender()),
        SenderDefinition(SenderType.CUSTOM_HTTP, CustomHttpSender())
    )

    fun all(): List<SenderDefinition> = definitions

    fun allTypes(): List<SenderType> = definitions.map { it.type }

    fun get(type: SenderType): SmsSender {
        return definitions.first { it.type == type }.sender
    }

    fun getConfigurable(type: SenderType): ConfigurableSender {
        return get(type) as? ConfigurableSender
            ?: error("Sender ${type.name} is not configurable")
    }
}
