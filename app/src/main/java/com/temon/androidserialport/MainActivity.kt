package com.temon.androidserialport

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.text.InputType
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.temon.serial.codec.HexCodec
import com.temon.serial.core.SerialException
import com.temon.serial.easy.EasySerial
import com.temon.serial.internal.serialport.SerialPortFinder
import com.temon.androidserialport.ScreenAdaptationUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 快速使用
 * Quick use
 * @author Vi
 * @date 2019-07-17 16:50
 * @e-mail cfop_f2l@163.com
 */
class MainActivity : Activity() {

    private var isOpenSerial = false
    private var currentPort: String = ""
    private var currentBaud: Int = 0
    private var hasPorts = false
    private var isConnecting = false
    private var autoScrollEnabled = true
    private var lastConnectError: String? = null
    private var showLogTime = true
    private var showLogTitle = true

    private lateinit var mBtnConnect: TextView
    private lateinit var mBtnSend: TextView
    private lateinit var mTvClearLog: TextView
    private lateinit var mSpPort: Spinner
    private lateinit var mSpBaud: Spinner
    private lateinit var mEtInput: EditText
    private lateinit var mTvStatus: TextView
    private lateinit var mRbHex: RadioButton
    private lateinit var mRbAscii: RadioButton
    private lateinit var mRvLogs: RecyclerView
    private lateinit var mTvEmpty: TextView
    private lateinit var mSwitchAutoScroll: Switch
    private lateinit var mSwitchTime: Switch
    private lateinit var mSwitchTitle: Switch
    private lateinit var mBtnLogSettings: ImageView
    private lateinit var mLogSettingsPanel: View
    private lateinit var mBtnCommonCommands: TextView
    private lateinit var commonCommandsController: CommonCommandsController

    private val portPaths = mutableListOf<String>()
    private val defaultBaudRates = listOf(
        1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400,
        57600, 76800, 115200, 153600, 230400, 307200, 460800, 921600
    )
    private val baudOptions = mutableListOf<BaudOption>()
    private lateinit var baudAdapter: ArrayAdapter<String>
    private var lastBaudSelection = 0
    private var ignoreBaudSelection = false
    private val serialPreferences by lazy { SerialPreferences(this) }
    private val logItems = mutableListOf<SerialLog>()
    private lateinit var logAdapter: SerialLogAdapter
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val defaultBaud = 9600
    private var isMockMode = false

    private val dataListener = EasySerial.OnDataReceivedListener { _, data, length ->
        val content = if (isHexMode()) {
            SerialInputUtils.formatHex(HexCodec.encode(data, 0, length))
        } else {
            String(data, 0, length)
        }
        addLog(Direction.RX, content)
    }
    private val errorListener = EasySerial.OnErrorListener { _, error, message, _ ->
        val tip = message?.takeIf { it.isNotBlank() } ?: error.name
        Toast.makeText(this@MainActivity, tip, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenAdaptationUtil.setCustomDensity(this, application)
        setContentView(R.layout.activity_main)
        mBtnConnect = findViewById<TextView>(R.id.mBtnConnect)
        mBtnSend = findViewById<TextView>(R.id.mBtnSend)
        mTvClearLog = findViewById<TextView>(R.id.mTvClearLog)
        mSpPort = findViewById<Spinner>(R.id.mSpPort)
        mSpBaud = findViewById<Spinner>(R.id.mSpBaud)
        mEtInput = findViewById<EditText>(R.id.mEtInput)
        mTvStatus = findViewById<TextView>(R.id.mTvStatus)
        mRbHex = findViewById<RadioButton>(R.id.mRbHex)
        mRbAscii = findViewById<RadioButton>(R.id.mRbAscii)
        mRvLogs = findViewById<RecyclerView>(R.id.mRvLogs)
        mTvEmpty = findViewById<TextView>(R.id.mTvEmpty)
        mSwitchAutoScroll = findViewById<Switch>(R.id.mSwitchAutoScroll)
        mSwitchTime = findViewById<Switch>(R.id.mSwitchTime)
        mSwitchTitle = findViewById<Switch>(R.id.mSwitchTitle)
        mBtnLogSettings = findViewById<ImageView>(R.id.mBtnLogSettings)
        mLogSettingsPanel = findViewById<View>(R.id.mLogSettingsPanel)
        mBtnCommonCommands = findViewById<TextView>(R.id.mBtnCommonCommands)
        loadLogPreferences()
        loadInputModePreference()
        mSwitchAutoScroll.isChecked = autoScrollEnabled
        mSwitchTime.isChecked = showLogTime
        mSwitchTitle.isChecked = showLogTitle
        commonCommandsController = CommonCommandsController(
            this,
            serialPreferences,
            onSendCommand = { command -> sendCommand(command.content, false, command.title) }
        )
        setupBaudRateSpinner()
        setupLogList()
        setupInputWatcher()
        EasySerial.onDataReceived(dataListener)
        EasySerial.onError(errorListener)
        initClick()
        loadSerialPorts()
        updateConnectionUi()
        refreshSendAvailability()
    }

    override fun onResume() {
        super.onResume()
        if (!isOpenSerial) {
            loadSerialPorts()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EasySerial.removeOnDataReceived(dataListener)
        EasySerial.removeOnError(errorListener)
        if (currentPort.isNotEmpty() && !isMockMode) {
            EasySerial.close(currentPort)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (mLogSettingsPanel.visibility == View.VISIBLE) {
                val panelRect = Rect()
                val buttonRect = Rect()
                mLogSettingsPanel.getGlobalVisibleRect(panelRect)
                mBtnLogSettings.getGlobalVisibleRect(buttonRect)
                val touchX = ev.rawX.toInt()
                val touchY = ev.rawY.toInt()
                if (!panelRect.contains(touchX, touchY) && !buttonRect.contains(touchX, touchY)) {
                    mLogSettingsPanel.visibility = View.GONE
                }
            }
            val focusedView = currentFocus
            if (focusedView is EditText) {
                val rect = Rect()
                focusedView.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focusedView.clearFocus()
                    hideKeyboard(focusedView)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupBaudRateSpinner() {
        rebuildBaudOptions()
        baudAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            baudOptions.map { it.label }
        )
        baudAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        mSpBaud.adapter = baudAdapter
        applyPreferredBaudSelection()
        mSpBaud.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (ignoreBaudSelection) return
                val option = baudOptions.getOrNull(position) ?: return
                if (option.value == null) {
                    showCustomBaudDialog()
                } else {
                    lastBaudSelection = position
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun setupLogList() {
        logAdapter = SerialLogAdapter(logItems) { log ->
            showLogActionSheet(log)
        }
        mRvLogs.layoutManager = LinearLayoutManager(this)
        mRvLogs.adapter = logAdapter
        logAdapter.setShowTime(mSwitchTime.isChecked)
        logAdapter.setShowTitle(mSwitchTitle.isChecked)
        updateEmptyView()
    }

    private fun setupInputWatcher() {
        mEtInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                refreshSendAvailability()
            }
        })
    }

    private fun initClick() {
        mBtnConnect.setOnClickListener {
            if (isConnecting) {
                return@setOnClickListener
            }
            if (!isOpenSerial) {
                if (!hasPorts) {
                    Toast.makeText(
                        this@MainActivity,
                        resources.getString(R.string.text_no_ports),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                val selectedPort = portPaths.getOrNull(mSpPort.selectedItemPosition).orEmpty()
                val baudRate = getSelectedBaudRate()
                if (selectedPort.isBlank()) {
                    Toast.makeText(
                        this@MainActivity,
                        resources.getString(R.string.text_full_data),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                isConnecting = true
                lastConnectError = null
                updateConnectionUi()
                if (isMockMode) {
                    isConnecting = false
                    currentPort = resources.getString(R.string.text_mock_port)
                    currentBaud = baudRate
                    isOpenSerial = true
                    lastConnectError = null
                    saveLastSuccessfulPort(currentPort)
                    saveLastSuccessfulBaud(currentBaud)
                    Toast.makeText(
                        this@MainActivity,
                        resources.getString(R.string.text_open_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val openResult = EasySerial.open(selectedPort, baudRate)
                    isConnecting = false
                    if (openResult == EasySerial.OPEN_OK) {
                        currentPort = selectedPort
                        currentBaud = baudRate
                        isOpenSerial = true
                        lastConnectError = null
                        saveLastSuccessfulPort(currentPort)
                        saveLastSuccessfulBaud(currentBaud)
                        Toast.makeText(
                            this@MainActivity,
                            resources.getString(R.string.text_open_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        isOpenSerial = false
                        currentPort = ""
                        currentBaud = 0
                        val reason = openErrorReason(openResult)
                        lastConnectError = reason
                        Toast.makeText(
                            this@MainActivity,
                            String.format(resources.getString(R.string.text_open_fail), openResult),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                isOpenSerial = false
                if (currentPort.isNotEmpty()) {
                    if (!isMockMode) {
                        EasySerial.close(currentPort)
                    }
                    currentPort = ""
                    currentBaud = 0
                }
                lastConnectError = null
            }
            updateConnectionUi()
            refreshSendAvailability()
        }

        mBtnSend.setOnClickListener {
            val input = mEtInput.text.toString()
            sendCommand(input, true)
        }

        mTvClearLog.setOnClickListener {
            logAdapter.clear()
            updateEmptyView()
        }

        mRbHex.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                saveInputModePreference(true)
            }
            refreshSendAvailability()
        }
        mRbAscii.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                saveInputModePreference(false)
            }
            refreshSendAvailability()
        }

        mSwitchAutoScroll.setOnCheckedChangeListener { _, isChecked ->
            autoScrollEnabled = isChecked
            saveLogPreferences()
        }
        mSwitchTime.setOnCheckedChangeListener { _, isChecked ->
            logAdapter.setShowTime(isChecked)
            showLogTime = isChecked
            saveLogPreferences()
        }
        mSwitchTitle.setOnCheckedChangeListener { _, isChecked ->
            logAdapter.setShowTitle(isChecked)
            showLogTitle = isChecked
            saveLogPreferences()
        }
        mBtnLogSettings.setOnClickListener {
            toggleLogSettingsPanel()
        }
        mTvStatus.setOnClickListener {
            if (isMockMode) {
                toggleMockMode(false)
                Toast.makeText(this, "已关闭模拟串口", Toast.LENGTH_SHORT).show()
            }
        }
        mTvStatus.setOnLongClickListener {
            if (isMockMode || !hasPorts) {
                val next = !isMockMode
                toggleMockMode(next)
                val tip = if (next) "已开启模拟串口" else "已关闭模拟串口"
                Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }

        mBtnCommonCommands.setOnClickListener {
            commonCommandsController.show()
        }
    }

    private fun toggleLogSettingsPanel() {
        mLogSettingsPanel.visibility = if (mLogSettingsPanel.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun loadLogPreferences() {
        autoScrollEnabled = serialPreferences.getLogAutoScroll()
        showLogTime = serialPreferences.getLogShowTime()
        showLogTitle = serialPreferences.getLogShowTitle()
    }

    private fun saveLogPreferences() {
        serialPreferences.setLogAutoScroll(autoScrollEnabled)
        serialPreferences.setLogShowTime(showLogTime)
        serialPreferences.setLogShowTitle(showLogTitle)
    }

    private fun loadInputModePreference() {
        val isHex = serialPreferences.getInputModeHex()
        mRbHex.isChecked = isHex
        mRbAscii.isChecked = !isHex
    }

    private fun saveInputModePreference(isHex: Boolean) {
        serialPreferences.setInputModeHex(isHex)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (commonCommandsController.handleImportResult(requestCode, resultCode, data)) {
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (commonCommandsController.handlePermissionResult(requestCode, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadSerialPorts() {
        portPaths.clear()
        if (isMockMode) {
            hasPorts = true
            portPaths.add(resources.getString(R.string.text_mock_port))
        } else {
            val ports = SerialPortFinder().getAllDevicesPath().toList()
            hasPorts = ports.isNotEmpty()
            if (hasPorts) {
                portPaths.addAll(ports)
            } else {
                portPaths.add(resources.getString(R.string.text_no_ports))
            }
        }
        val portAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            portPaths
        )
        portAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        mSpPort.adapter = portAdapter
        applyPreferredPortSelection()
        mBtnConnect.isEnabled = hasPorts
        mSpPort.isEnabled = hasPorts && !isOpenSerial
    }

    private fun updateConnectionUi() {
        if (isConnecting) {
            mTvStatus.text = resources.getString(R.string.text_status_connecting)
            mTvStatus.setTextColor(ContextCompat.getColor(this, R.color.textHint))
            mBtnConnect.text = resources.getString(R.string.text_connecting)
            mBtnConnect.isEnabled = false
        } else if (!hasPorts) {
            mTvStatus.text = resources.getString(R.string.text_status_no_ports)
            mTvStatus.setTextColor(ContextCompat.getColor(this, R.color.textDelete))
            mBtnConnect.text = resources.getString(R.string.text_connect)
            mBtnConnect.isEnabled = false
        } else if (isOpenSerial && currentPort.isNotEmpty()) {
            mTvStatus.text = resources.getString(
                R.string.text_status_connected,
                currentPort,
                currentBaud
            )
            mTvStatus.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            mBtnConnect.text = resources.getString(R.string.text_disconnect)
            mBtnConnect.isEnabled = true
        } else if (lastConnectError != null) {
            mTvStatus.text = resources.getString(
                R.string.text_status_failed,
                lastConnectError
            )
            mTvStatus.setTextColor(ContextCompat.getColor(this, R.color.textDelete))
            mBtnConnect.text = resources.getString(R.string.text_connect)
            mBtnConnect.isEnabled = true
        } else {
            mTvStatus.text = resources.getString(R.string.text_status_idle)
            mTvStatus.setTextColor(ContextCompat.getColor(this, R.color.textSecondary))
            mBtnConnect.text = resources.getString(R.string.text_connect)
            mBtnConnect.isEnabled = true
        }
        val canEdit = !isOpenSerial && !isConnecting && hasPorts
        mSpPort.isEnabled = canEdit
        mSpBaud.isEnabled = !isOpenSerial && !isConnecting
        val sendEnabled = isOpenSerial && !isConnecting
        mEtInput.isEnabled = sendEnabled
        mRbHex.isEnabled = sendEnabled
        mRbAscii.isEnabled = sendEnabled
        if (!sendEnabled) {
            mEtInput.error = null
        }
        mBtnSend.isEnabled = sendEnabled && isValidSendInput()
    }

    private fun toggleMockMode(enabled: Boolean) {
        if (isOpenSerial && currentPort.isNotEmpty()) {
            if (!isMockMode) {
                EasySerial.close(currentPort)
            }
            isOpenSerial = false
            currentPort = ""
            currentBaud = 0
        }
        isMockMode = enabled
        lastConnectError = null
        loadSerialPorts()
        updateConnectionUi()
        refreshSendAvailability()
    }

    private fun isHexMode(): Boolean = mRbHex.isChecked

    private fun refreshSendAvailability() {
        mBtnSend.isEnabled = isOpenSerial && !isConnecting && isValidSendInput()
    }

    private fun rebuildBaudOptions() {
        baudOptions.clear()
        defaultBaudRates.sorted().forEach { rate ->
            baudOptions.add(BaudOption(rate, rate.toString()))
        }
        baudOptions.add(BaudOption(null, resources.getString(R.string.text_baud_custom)))
    }

    private fun applyPreferredBaudSelection() {
        val preferredBaud = loadLastSuccessfulBaud() ?: defaultBaud
        ensureBaudOption(preferredBaud)
        val selectedIndex = baudOptions.indexOfFirst { it.value == preferredBaud }
        if (selectedIndex >= 0) {
            setBaudSelection(selectedIndex)
            lastBaudSelection = selectedIndex
        }
    }

    private fun ensureBaudOption(baud: Int) {
        val exists = baudOptions.any { it.value == baud }
        if (!exists) {
            val insertIndex = (baudOptions.size - 1).coerceAtLeast(0)
            baudOptions.add(insertIndex, BaudOption(baud, baud.toString()))
            refreshBaudAdapter()
        }
    }

    private fun refreshBaudAdapter() {
        if (!::baudAdapter.isInitialized) return
        baudAdapter.clear()
        baudAdapter.addAll(baudOptions.map { it.label })
        baudAdapter.notifyDataSetChanged()
    }

    private fun setBaudSelection(position: Int) {
        ignoreBaudSelection = true
        mSpBaud.setSelection(position)
        mSpBaud.post { ignoreBaudSelection = false }
    }

    private fun showCustomBaudDialog() {
        // Create a container to add left/right padding for the EditText,
        // matching AlertDialog's title horizontal padding (typically 24dp).
        val horizontalPadding = (24 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            hint = resources.getString(R.string.text_rate_input)
            inputType = InputType.TYPE_CLASS_NUMBER
            keyListener = DigitsKeyListener.getInstance("0123456789")
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(9))
            setBackgroundResource(R.drawable.bg_input)
            // Add left and right padding (e.g., 16dp)
            val lrPadding = (16 * resources.displayMetrics.density).toInt()
            setPadding(lrPadding, paddingTop, lrPadding, paddingBottom)
        }
        val container = android.widget.FrameLayout(this).apply {
            setPadding(horizontalPadding, horizontalPadding, horizontalPadding, 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.text_baud_custom))
            .setView(container)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                setBaudSelection(lastBaudSelection)
            }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val raw = input.text.toString().trim()
                if (raw.isBlank() || raw.any { !it.isDigit() }) {
                    input.error = resources.getString(R.string.text_baud_invalid)
                    return@setOnClickListener
                }
                val baud = raw.toIntOrNull()
                if (baud == null || baud <= 0) {
                    input.error = resources.getString(R.string.text_baud_invalid)
                    return@setOnClickListener
                }
                ensureBaudOption(baud)
                val index = baudOptions.indexOfFirst { it.value == baud }
                if (index >= 0) {
                    setBaudSelection(index)
                    lastBaudSelection = index
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun getSelectedBaudRate(): Int {
        val option = baudOptions.getOrNull(mSpBaud.selectedItemPosition)
        return option?.value ?: defaultBaud
    }

    private fun saveLastSuccessfulBaud(baud: Int) {
        serialPreferences.setLastSuccessfulBaud(baud)
    }

    private fun saveLastSuccessfulPort(port: String) {
        serialPreferences.setLastSuccessfulPort(port)
    }

    private fun loadLastSuccessfulBaud(): Int? {
        return serialPreferences.getLastSuccessfulBaud()
    }

    private fun loadLastSuccessfulPort(): String? {
        return serialPreferences.getLastSuccessfulPort()
    }

    private fun applyPreferredPortSelection() {
        val preferredPort = loadLastSuccessfulPort() ?: return
        val index = portPaths.indexOfFirst { it == preferredPort }
        if (index >= 0) {
            mSpPort.setSelection(index)
        }
    }

    private fun isValidSendInput(): Boolean {
        val input = mEtInput.text.toString()
        val valid = if (input.isBlank()) {
            false
        } else {
            if (isHexMode()) {
                SerialInputUtils.isValidHex(input)
            } else {
                true
            }
        }
        if (isHexMode() && input.isNotBlank() && !SerialInputUtils.isValidHex(input)) {
            mEtInput.error = resources.getString(R.string.text_hex_invalid)
        } else {
            mEtInput.error = null
        }
        return valid
    }

    private fun addLog(direction: Direction, content: String, title: String? = null) {
        val log = SerialLog(timeFormat.format(Date()), direction, content, title)
        runOnUiThread {
            logAdapter.add(log)
            if (autoScrollEnabled) {
                mRvLogs.scrollToPosition(logAdapter.itemCount - 1)
            }
            updateEmptyView()
        }
    }

    private fun simulateReceive(content: String) {
        mRvLogs.postDelayed({
            addLog(Direction.RX, content)
        }, 200L)
    }

    private fun formatLogLine(log: SerialLog): String {
        val arrow = if (log.direction == Direction.TX) "▶" else "◀"
        val dirText = if (log.direction == Direction.TX) "TX" else "RX"
        val title = log.title?.takeIf { it.isNotBlank() }
        val displayContent = if (title == null) {
            log.content
        } else {
            "$title: ${log.content}"
        }
        return "[${log.time}] $arrow $dirText: $displayContent"
    }

    private fun copyLogToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("serial-log", text))
        Toast.makeText(this, resources.getString(R.string.text_copy_success), Toast.LENGTH_SHORT)
            .show()
    }

    private fun showLogActionSheet(log: SerialLog) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(
            this,
            R.style.AppBottomSheetDialogTheme
        )
        val view = layoutInflater.inflate(R.layout.bottom_sheet_log_actions, null)
        val tvResend = view.findViewById<TextView>(R.id.mTvResend)
        val tvCopy = view.findViewById<TextView>(R.id.mTvCopy)
        val tvAddCommon = view.findViewById<TextView>(R.id.mTvAddCommon)
        val isTx = log.direction == Direction.TX
        val alpha = if (isTx) 1f else 0.4f
        tvResend.alpha = alpha
        tvAddCommon.alpha = alpha
        tvResend.isEnabled = isTx
        tvAddCommon.isEnabled = isTx
        tvResend.setOnClickListener {
            if (isTx) {
                sendCommand(log.content, false, log.title)
            }
            dialog.dismiss()
        }
        tvCopy.setOnClickListener {
            copyLogToClipboard(log.content)
            dialog.dismiss()
        }
        tvAddCommon.setOnClickListener {
            if (isTx) {
                commonCommandsController.addCommand(log.content)
            }
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet =
                (dialogInterface as? com.google.android.material.bottomsheet.BottomSheetDialog)
                    ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener
            bottomSheet.post {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        dialog.show()
    }

    private fun sendCommand(content: String, showInputError: Boolean, title: String? = null) {
        if (!isOpenSerial) {
            Toast.makeText(
                this@MainActivity,
                resources.getString(R.string.text_serial_open_fail),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (content.isBlank()) {
            Toast.makeText(
                this@MainActivity,
                resources.getString(R.string.text_full_send_Data),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (isHexMode() && !SerialInputUtils.isValidHex(content)) {
            if (showInputError) {
                mEtInput.error = resources.getString(R.string.text_hex_invalid)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    resources.getString(R.string.text_hex_invalid),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        try {
            if (currentPort.isNotEmpty()) {
                if (isHexMode()) {
                    val hex = SerialInputUtils.normalizeHex(content)
                    val formatted = SerialInputUtils.formatHex(hex)
                    if (isMockMode) {
                        addLog(Direction.TX, formatted, title)
                        simulateReceive(formatted)
                        clearInputAfterSend(showInputError)
                        return
                    }
                    EasySerial.send(currentPort, HexCodec.decode(hex))
                    addLog(Direction.TX, formatted, title)
                    clearInputAfterSend(showInputError)
                } else {
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    if (isMockMode) {
                        addLog(Direction.TX, content, title)
                        simulateReceive(content)
                        clearInputAfterSend(showInputError)
                        return
                    }
                    EasySerial.send(currentPort, bytes)
                    addLog(Direction.TX, content, title)
                    clearInputAfterSend(showInputError)
                }
            }
        } catch (e: SerialException) {
            Toast.makeText(
                this@MainActivity,
                "发送失败: ${e.error}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(
                this@MainActivity,
                "发送失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearInputAfterSend(shouldClear: Boolean) {
        if (!shouldClear) return
        mEtInput.setText("")
        mEtInput.error = null
        refreshSendAvailability()
        hideKeyboard(mEtInput)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun updateEmptyView() {
        mTvEmpty.visibility = if (logAdapter.itemCount == 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun openErrorReason(code: Int): String {
        return when (code) {
            EasySerial.OPEN_NO_PERMISSION -> resources.getString(R.string.text_reason_no_permission)
            EasySerial.OPEN_INVALID_PARAM -> resources.getString(R.string.text_reason_invalid_param)
            EasySerial.OPEN_UNKNOWN_ERROR -> resources.getString(R.string.text_reason_unknown)
            else -> resources.getString(R.string.text_reason_unknown)
        }
    }

    private data class BaudOption(val value: Int?, val label: String)
}
