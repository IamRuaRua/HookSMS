package top.cuteruarua.hooksms

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.concurrent.thread
import top.cuteruarua.hooksms.config.SenderConfigStore
import top.cuteruarua.hooksms.sender.ConfigFieldSpec
import top.cuteruarua.hooksms.sender.ConfigFieldType
import top.cuteruarua.hooksms.sender.ConfigurableSender
import top.cuteruarua.hooksms.sender.SenderRegistry
import top.cuteruarua.hooksms.sender.SenderType

class ConfigActivity : Activity() {

    companion object {
        private val COLOR_PAGE_BG = 0xFFFFFFFF.toInt()
        private val COLOR_TOP_BAR = 0xFFFFFFFF.toInt()
        private val COLOR_SURFACE = 0xFFF8FCFF.toInt()
        private val COLOR_INPUT = 0xFFE1F3FF.toInt()
        private val COLOR_STROKE = 0xFFBBDEFB.toInt()
        private val COLOR_INK = 0xFF20262A.toInt()
        private val COLOR_MUTED = 0xFF455A64.toInt()
        private val COLOR_ACCENT = 0xFF1976D2.toInt()
        private val COLOR_ACCENT_SOFT = 0xFFBBDEFB.toInt()
        private val COLOR_LOG_PANEL = 0xFFF8FCFF.toInt()
        private val COLOR_LOG_TEXT = 0xFF37474F.toInt()
    }

    private lateinit var configTypeSpinner: Spinner
    private lateinit var viewPager: ViewPager2
    private lateinit var configFieldsContainer: LinearLayout
    private lateinit var logText: TextView
    private lateinit var topBarTitle: TextView
    private lateinit var clearButton: ImageButton

    private val senderTypes = SenderRegistry.allTypes()
    private var currentSender: ConfigurableSender? = null
    private var currentFieldSpecs: List<ConfigFieldSpec> = emptyList()
    private val fieldViews = linkedMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createUI()
        loadConfig()
    }

    private fun createUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_PAGE_BG)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(COLOR_TOP_BAR)
            setPadding(dp(24), 0, dp(16), 0)
        }

        topBarTitle = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.create("serif", Typeface.BOLD)
            setTextColor(COLOR_INK)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        }
        topBar.addView(topBarTitle)

        clearButton = ImageButton(this).apply {
            contentDescription = "Clear logs"
            visibility = View.INVISIBLE
            setImageResource(R.drawable.ic_broom_24)
            setColorFilter(COLOR_ACCENT)
            setBackgroundColor(0x00000000)
            backgroundTintList = null
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setOnClickListener { clearLogs() }
        }
        topBar.addView(clearButton)

        mainLayout.addView(topBar)

        viewPager = ViewPager2(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        mainLayout.addView(viewPager)

        val tabLayout = TabLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL
            setBackgroundColor(COLOR_TOP_BAR)
            setSelectedTabIndicatorColor(COLOR_ACCENT)
            setTabTextColors(COLOR_MUTED, COLOR_ACCENT)
        }
        mainLayout.addView(tabLayout)

        setContentView(mainLayout)

        val pages = listOf(createConfigPage(), createLogPage())
        viewPager.adapter = PageAdapter(pages)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Config" else "Logs"
        }.attach()

        updatePageTitle(0)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageTitle(position)
                if (position == 1) {
                    loadLogs()
                }
            }
        })

        setupConfigTypeListener()
        loadLogs()
    }

    private fun updatePageTitle(position: Int) {
        val pageTitle = if (position == 0) "Module Config" else "Module Logs"
        title = pageTitle
        topBarTitle.text = pageTitle
        clearButton.visibility = if (position == 1) View.VISIBLE else View.INVISIBLE
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun roundedRect(
        color: Int,
        radiusDp: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null && strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun createConfigPage(): ScrollView {
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(COLOR_PAGE_BG)
            clipToPadding = false
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(COLOR_SURFACE, 26, COLOR_STROKE, 1)
            setPadding(dp(22), dp(22), dp(22), dp(20))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(18), dp(16), dp(24))
            }
        }
        scrollView.addView(mainLayout)

        mainLayout.addView(createLabel("Config Type"))

        configTypeSpinner = Spinner(this).apply {
            background = roundedRect(COLOR_INPUT, 8, COLOR_STROKE, 1)
            backgroundTintList = null
            setPadding(dp(12), 12, dp(12), 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(5)) }
        }
        mainLayout.addView(configTypeSpinner)

        configFieldsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        mainLayout.addView(configFieldsContainer)

        mainLayout.addView(createActionButton("Save Config", true) { saveConfig() })
        mainLayout.addView(createActionButton("Test Connection", false) { testConnection() })

        return scrollView
    }

    private fun createLogPage(): ScrollView {
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(COLOR_PAGE_BG)
            isFillViewport = true
            clipToPadding = false
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(COLOR_LOG_PANEL, 24, COLOR_STROKE, 1)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(16), dp(18), dp(16), dp(24))
            }
        }

        logText = TextView(this).apply {
            textSize = 12.5f
            setTextColor(COLOR_LOG_TEXT)
            setTypeface(android.graphics.Typeface.MONOSPACE)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        container.addView(logText)

        scrollView.addView(container)
        return scrollView
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0.08f
            setTextColor(COLOR_MUTED)
            setPadding(0, dp(4), 0, dp(5))
        }
    }

    private fun createActionButton(
        text: String,
        primary: Boolean,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            setAllCaps(false)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 15f
            minHeight = dp(36)
            setTextColor(if (primary) COLOR_SURFACE else COLOR_ACCENT)
            background = if (primary) {
                roundedRect(COLOR_ACCENT, 24)
            } else {
                roundedRect(COLOR_ACCENT_SOFT, 24, COLOR_ACCENT, 1)
            }
            backgroundTintList = null
            stateListAnimator = null
            elevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(38)
            ).apply {
                setMargins(0, dp(10), 0, 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSpinnerAdapter(items: Array<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return styleSpinnerText(super.getView(position, convertView, parent))
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return styleSpinnerText(super.getDropDownView(position, convertView, parent))
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun styleSpinnerText(view: View): View {
        (view as? TextView)?.apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 14f
            setTextColor(COLOR_INK)
            setPadding(dp(2), 0, dp(2), 0)
        }
        return view
    }

    private fun setupConfigTypeListener() {
        val types = senderTypes.map { it.displayName }.toTypedArray()
        configTypeSpinner.adapter = createSpinnerAdapter(types)

        configTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                renderConfigFields(senderTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun renderConfigFields(type: SenderType) {
        currentSender = SenderRegistry.getConfigurable(type)
        currentFieldSpecs = currentSender?.getConfigFields().orEmpty()
        val savedValues = currentSender?.let { SenderConfigStore.load(this, it) }.orEmpty()

        fieldViews.clear()
        configFieldsContainer.removeAllViews()

        currentFieldSpecs.forEach { spec ->
            configFieldsContainer.addView(createLabel(spec.label))
            val fieldView = createFieldView(spec, savedValues)
            fieldViews[spec.key] = fieldView
            configFieldsContainer.addView(fieldView)
        }

        refreshDynamicHints()
    }

    private fun createFieldView(spec: ConfigFieldSpec, savedValues: Map<String, String>): View {
        return when (spec.type) {
            ConfigFieldType.SELECT -> {
                Spinner(this).apply {
                    background = roundedRect(COLOR_INPUT, 8, COLOR_STROKE, 1)
                    backgroundTintList = null
                    setPadding(dp(12), 0, dp(12), 0)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, dp(1)) }

                    val labels = spec.options.map { it.label }
                    adapter = createSpinnerAdapter(labels.toTypedArray())

                    val selectedValue = savedValues[spec.key]
                    val selectedIndex = spec.options.indexOfFirst { it.value == selectedValue }
                        .takeIf { it >= 0 } ?: 0
                    setSelection(selectedIndex)

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            refreshDynamicHints()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            }

            else -> {
                EditText(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dp(10))
                    }
                    background = roundedRect(COLOR_INPUT, 8, COLOR_STROKE, 1)
                    backgroundTintList = null
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    textSize = 15f
                    setTextColor(COLOR_INK)
                    setHintTextColor(COLOR_MUTED)
                    hint = spec.hint
                    setText(savedValues[spec.key].orEmpty())
                    inputType = when (spec.type) {
                        ConfigFieldType.PASSWORD -> {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }

                        ConfigFieldType.NUMBER -> InputType.TYPE_CLASS_NUMBER
                        ConfigFieldType.MULTILINE -> {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        }

                        else -> InputType.TYPE_CLASS_TEXT
                    }
                    if (spec.type == ConfigFieldType.MULTILINE) {
                        minLines = 3
                        gravity = Gravity.TOP
                    }
                }
            }
        }
    }

    private fun refreshDynamicHints() {
        val values = collectFieldValues()
        currentFieldSpecs.forEach { spec ->
            val hint = spec.hintProvider?.invoke(values) ?: spec.hint
            val editText = fieldViews[spec.key] as? EditText ?: return@forEach
            editText.hint = hint
        }
    }

    private fun collectFieldValues(): Map<String, String> {
        val values = linkedMapOf<String, String>()
        currentFieldSpecs.forEach { spec ->
            val view = fieldViews[spec.key] ?: return@forEach
            val value = when (spec.type) {
                ConfigFieldType.SELECT -> {
                    val spinner = view as Spinner
                    val index = spinner.selectedItemPosition
                    spec.options.getOrNull(index)?.value.orEmpty()
                }

                else -> (view as EditText).text.toString()
            }
            values[spec.key] = value
        }
        return values
    }

    private fun loadConfig() {
        val type = SenderConfigStore.loadSelectedSenderType(this)
        val typeIndex = senderTypes.indexOf(type)
        if (typeIndex >= 0) {
            configTypeSpinner.setSelection(typeIndex)
        }
        renderConfigFields(type)
    }

    private fun saveConfig() {
        val sender = currentSender ?: return
        val selectedType = senderTypes[configTypeSpinner.selectedItemPosition]
        val values = collectFieldValues()
        val error = sender.validateConfigValues(values)

        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            return
        }

        SenderConfigStore.saveSelectedSenderType(this, selectedType)
        SenderConfigStore.save(this, sender, values)

        Toast.makeText(this, "Config saved", Toast.LENGTH_SHORT).show()
    }

    private fun testConnection() {
        val sender = currentSender ?: return
        val selectedType = senderTypes[configTypeSpinner.selectedItemPosition]
        val values = collectFieldValues()
        val error = sender.validateConfigValues(values)

        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show()
        LogManager.log("========== Testing ${selectedType.displayName} ==========", this)

        thread {
            try {
                val result = sender.testConnection(values)
                runOnUiThread {
                    LogManager.log("Test response:\n${result.detail}", this)
                    if (result.success) {
                        LogManager.log("Test success", this)
                        Toast.makeText(this, "Test success", Toast.LENGTH_LONG).show()
                    } else {
                        LogManager.log("Test failed, please check response", this)
                        Toast.makeText(this, "Test failed, please check config", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                showTestFailure(e)
            } catch (e: LinkageError) {
                showTestFailure(e)
            }
        }
    }

    private fun showTestFailure(error: Throwable) {
        val message = formatError(error)
        runOnUiThread {
            LogManager.log("Test error:\n$message", this)
            Toast.makeText(this, "Test failed: ${error.message ?: error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatError(error: Throwable): String {
        val message = error.message ?: "No message"
        val stack = error.stackTrace
            .take(6)
            .joinToString("\n") { "  at $it" }
        return buildString {
            append("${error.javaClass.simpleName}: $message")
            if (stack.isNotBlank()) {
                append("\n")
                append(stack)
            }
        }
    }

    private fun clearLogs() {
        LogManager.clearLogs(this)
        loadLogs()
        Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
    }

    private fun loadLogs() {
        val logPrefs = getSharedPreferences("hooksms_logs", Context.MODE_PRIVATE)
        val logs = logPrefs.getString("logs", "") ?: ""

        logText.text = if (logs.isEmpty()) {
            "No logs yet\n\nRun a connection test and diagnostics will appear here."
        } else {
            logs
        }

        (logText.parent as? ViewGroup)?.let { container ->
            (container.parent as? ScrollView)?.post {
                (container.parent as? ScrollView)?.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}

private class PageAdapter(
    private val pages: List<View>
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        return PageViewHolder(pages[viewType])
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = pages.size

    override fun getItemViewType(position: Int): Int = position

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
