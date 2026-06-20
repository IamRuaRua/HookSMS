package top.cuteruarua.hooksms.config

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import java.io.File
import top.cuteruarua.hooksms.sender.ConfigurableSender
import top.cuteruarua.hooksms.sender.SenderType

object SenderConfigStore {

    const val MODULE_PACKAGE = "top.cuteruarua.hooksms"
    private const val ROOT_PREFS_NAME = "hooksms_config"
    private const val KEY_SENDER_TYPE = "config_type"

    @Suppress("DEPRECATION")
    fun load(context: Context, sender: ConfigurableSender): Map<String, String> {
        val prefs = context.getSharedPreferences(getPrefsName(sender), Context.MODE_WORLD_READABLE)
        return sender.getConfigFields().associate { field ->
            field.key to (prefs.getString(field.key, "") ?: "")
        }
    }

    @Suppress("DEPRECATION")
    fun save(context: Context, sender: ConfigurableSender, values: Map<String, String>) {
        val storedValues = buildStoredValues(sender, values)
        context.getSharedPreferences(getPrefsName(sender), Context.MODE_WORLD_READABLE)
            .edit()
            .apply {
                storedValues.forEach { (key, value) ->
                    putString(key, value)
                }
            }
            .commit()
        ensurePrefsReadable(context, getPrefsName(sender))
    }

    fun loadSelectedSenderType(context: Context): SenderType {
        val prefs = openWorldReadablePrefs(context, ROOT_PREFS_NAME)
        val savedType = prefs.getString(KEY_SENDER_TYPE, SenderType.QQ_NAPCAT.name)
        return SenderType.entries.firstOrNull { it.name == savedType } ?: SenderType.QQ_NAPCAT
    }

    fun loadForHook(sender: ConfigurableSender): Map<String, String> {
        val primaryName = getPrefsName(sender)
        val primaryValues = readHookValues(primaryName, sender) { fieldKey -> fieldKey }
        return if (isMeaningful(primaryValues)) {
            primaryValues
        } else {
            val legacyName = legacyPrefsName(sender)
            if (legacyName != null) {
                val legacyValues = readHookValues(legacyName, sender) { fieldKey -> fieldKey }
                if (isMeaningful(legacyValues)) {
                    legacyValues
                } else {
                    primaryValues
                }
            } else {
                primaryValues
            }
        }
    }

    fun loadSelectedSenderTypeForHook(): SenderType {
        val prefs = XSharedPreferences(MODULE_PACKAGE, ROOT_PREFS_NAME).apply { reload() }
        val savedType = prefs.getString(KEY_SENDER_TYPE, SenderType.QQ_NAPCAT.name)
        return SenderType.entries.firstOrNull { it.name == savedType } ?: SenderType.QQ_NAPCAT
    }

    @Suppress("DEPRECATION")
    fun saveSelectedSenderType(context: Context, senderType: SenderType) {
        context.getSharedPreferences(ROOT_PREFS_NAME, Context.MODE_WORLD_READABLE)
            .edit()
            .putString(KEY_SENDER_TYPE, senderType.name)
            .commit()
        ensurePrefsReadable(context, ROOT_PREFS_NAME)
    }

    fun buildStoredValues(
        sender: ConfigurableSender,
        values: Map<String, String>
    ): Map<String, String> {
        return sender.getConfigFields().associate { field ->
            field.key to values[field.key].orEmpty()
        }
    }

    private fun getPrefsName(sender: ConfigurableSender): String {
        return "${ROOT_PREFS_NAME}_${sender.configName}"
    }

    private fun legacyPrefsName(sender: ConfigurableSender): String? {
        return when (sender.configName) {
            "qq_napcat" -> "hooksms_config_qq"
            "custom_http" -> "hooksms_config_custom"
            else -> null
        }
    }

    private fun readHookValues(
        prefsName: String,
        sender: ConfigurableSender,
        keyMapper: (String) -> String
    ): Map<String, String> {
        val prefs = XSharedPreferences(MODULE_PACKAGE, prefsName).apply { reload() }
        return sender.getConfigFields().associate { field ->
            field.key to (prefs.getString(keyMapper(field.key), "") ?: "")
        }
    }

    private fun isMeaningful(values: Map<String, String>): Boolean {
        return values.values.any { it.isNotBlank() }
    }

    private fun ensurePrefsReadable(context: Context, prefsName: String) {
        runCatching {
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false)
                prefsDir.setExecutable(true, false)
            }

            val prefsFile = File(prefsDir, "$prefsName.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun openWorldReadablePrefs(context: Context, prefsName: String) =
        context.getSharedPreferences(prefsName, Context.MODE_WORLD_READABLE)
}
