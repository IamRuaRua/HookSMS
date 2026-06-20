package top.cuteruarua.hooksms

import android.content.Intent
import android.telephony.SmsMessage
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.cuteruarua.hooksms.config.SenderConfigStore
import top.cuteruarua.hooksms.sender.ConfigFieldType
import top.cuteruarua.hooksms.sender.ConfigurableSender
import top.cuteruarua.hooksms.sender.SenderRegistry
import top.cuteruarua.hooksms.sender.SmsData
import top.cuteruarua.hooksms.sender.SmsSender

class XposedEntry : IXposedHookLoadPackage {

    private val targetPackages = setOf("com.android.mms")

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in targetPackages) {
            return
        }

        XposedBridge.log("HookSMS: Module loaded, processing package: ${lpparam.packageName}")
        logCurrentConfig()
        hookGetMessagesFromIntent(lpparam)
    }

    private fun logCurrentConfig() {
        runCatching {
            val configType = SenderConfigStore.loadSelectedSenderTypeForHook()
            XposedBridge.log("HookSMS: Active sender type=${configType.displayName}")

            val sender = SenderRegistry.get(configType) as? ConfigurableSender
            if (sender == null) {
                XposedBridge.log("HookSMS: Active sender is not configurable")
                return
            }

            val values = SenderConfigStore.loadForHook(sender)
            sender.getConfigFields().forEach { field ->
                val rawValue = values[field.key].orEmpty()
                val safeValue = when (field.type) {
                    ConfigFieldType.PASSWORD -> "len=${rawValue.length}"
                    else -> rawValue.ifBlank { "<blank>" }
                }
                XposedBridge.log("HookSMS: Config ${sender.configName}.${field.key}=$safeValue")
            }
        }.onFailure { error ->
            XposedBridge.log("HookSMS: Print config failed - ${error.message}")
        }
    }

    private fun hookGetMessagesFromIntent(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val smsIntentsClass = XposedHelpers.findClass(
                "android.provider.Telephony\$Sms\$Intents",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                smsIntentsClass,
                "getMessagesFromIntent",
                Intent::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val messages = (param.result as? Array<*>)?.filterIsInstance<SmsMessage>().orEmpty()
                            if (messages.isNotEmpty()) {
                                logSmsMessages(messages, "getMessagesFromIntent", lpparam)
                            }
                        } catch (e: Exception) {
                            XposedBridge.log("HookSMS: getMessagesFromIntent parse failed - ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("HookSMS: Hooked getMessagesFromIntent successfully")
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: Hook getMessagesFromIntent failed - ${e.message}")
        }
    }

    private fun logSmsMessages(
        smsMessages: List<SmsMessage>,
        source: String,
        lpparam: XC_LoadPackage.LoadPackageParam
    ) {
        try {
            val mergedSms = SmsSegmentMerger.merge(
                smsMessages.map { smsMessage ->
                    SmsSegment(
                        originatingAddress = try {
                            smsMessage.originatingAddress
                        } catch (e: Exception) {
                            XposedBridge.log("HookSMS: Read sender failed - ${e.message}")
                            "unknown"
                        },
                        messageBody = try {
                            smsMessage.messageBody ?: smsMessage.displayMessageBody
                        } catch (e: Exception) {
                            XposedBridge.log("HookSMS: Read message body failed - ${e.message}")
                            "read failed"
                        },
                        timestampMillis = try {
                            smsMessage.timestampMillis
                        } catch (e: Exception) {
                            XposedBridge.log("HookSMS: Read timestamp failed - ${e.message}")
                            System.currentTimeMillis()
                        }
                    )
                }
            ) ?: return

            val timestamp = try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                dateFormat.format(Date(mergedSms.timestampMillis))
            } catch (e: Exception) {
                "time format failed: ${mergedSms.timestampMillis}"
            }

            XposedBridge.log("HookSMS: ========== Intercepted SMS [$source] ==========")
            XposedBridge.log(
                "HookSMS: Sender=${mergedSms.originatingAddress}, segments=${mergedSms.segmentCount}, length=${mergedSms.messageBody.length}"
            )
            XposedBridge.log("HookSMS: Body: ${mergedSms.messageBody}")
            XposedBridge.log("HookSMS: Time: $timestamp")
            XposedBridge.log("HookSMS: ===============================================")

            try {
                val currentSender = getCurrentSender(lpparam)
                if (currentSender != null) {
                    val data = SmsData(
                        originatingAddress = mergedSms.originatingAddress,
                        messageBody = mergedSms.messageBody,
                        timestamp = timestamp
                    )
                    val result = currentSender.send(lpparam, data)
                    XposedBridge.log("HookSMS: Send result - ${result.message}")
                } else {
                    XposedBridge.log("HookSMS: No valid sender config found")
                }
            } catch (e: Exception) {
                XposedBridge.log("HookSMS: Send failed - ${e.message}")
            }
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: Record SMS failed - ${e.message}")
            e.stackTrace.take(5).forEach {
                XposedBridge.log("HookSMS:   at $it")
            }
        }
    }

    private fun getCurrentSender(lpparam: XC_LoadPackage.LoadPackageParam): SmsSender? {
        return try {
            val configType = SenderConfigStore.loadSelectedSenderTypeForHook()
            XposedBridge.log("HookSMS: Using sender type: ${configType.displayName}")
            SenderRegistry.get(configType)
        } catch (e: Exception) {
            XposedBridge.log("HookSMS: Get sender failed - ${e.message}")
            null
        }
    }
}
