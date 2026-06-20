package top.cuteruarua.hooksms.sender

enum class SenderType(val displayName: String, val description: String) {
    QQ_NAPCAT("QQ NapCat", "Send to QQ group via NapCat"),
    CUSTOM_HTTP("Custom HTTP", "Send via a custom HTTP request"),
    FEISHU_BOT("Feishu Bot", "Send text messages via a Feishu bot webhook")
}
