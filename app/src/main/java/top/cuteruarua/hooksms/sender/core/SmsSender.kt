package top.cuteruarua.hooksms.sender

import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 短信发送器接口 - 所有发送器必须实现此接口
 */
interface SmsSender {
    /**
     * 发送短信
     * @param lpparam Xposed 加载包参数
     * @param data 短信数据
     * @return 发送结果
     */
    fun send(lpparam: XC_LoadPackage.LoadPackageParam, data: SmsData): SendResult
}

/**
 * 短信数据
 */
data class SmsData(
    val originatingAddress: String?,  // 发送号码
    val messageBody: String?,          // 短信内容
    val timestamp: String,             // 时间戳
    val simSlot: String? = null,       // SIM卡槽位（可选）
    val recipientNumber: String? = null // 接收号码（可选）
)

/**
 * 发送结果
 */
data class SendResult(
    val success: Boolean,
    val message: String,
    val detail: String? = null
)
