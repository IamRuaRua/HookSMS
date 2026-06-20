package top.cuteruarua.hooksms.sender

enum class ConfigFieldType {
    TEXT,
    PASSWORD,
    NUMBER,
    MULTILINE,
    SELECT
}

data class ConfigFieldOption(
    val value: String,
    val label: String
)

data class ConfigFieldSpec(
    val key: String,
    val label: String,
    val type: ConfigFieldType,
    val hint: String = "",
    val options: List<ConfigFieldOption> = emptyList(),
    val hintProvider: ((Map<String, String>) -> String)? = null
)

data class ConfigTestResult(
    val success: Boolean,
    val detail: String
)

interface ConfigurableSender {
    val configName: String
    fun getConfigFields(): List<ConfigFieldSpec>
    fun validateConfigValues(values: Map<String, String>): String?
    fun testConnection(values: Map<String, String>): ConfigTestResult
}
