package com.temon.androidserialport

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.temon.serial.codec.HexCodec
import com.temon.serial.core.SerialException
import com.temon.serial.easy.EasySerial
import com.temon.serial.internal.serialport.SerialPortFinder
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
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

    private lateinit var mBtnConnect: Button
    private lateinit var mBtnSend: Button
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
    private lateinit var mTvCommonCommands: TextView

    private val portPaths = mutableListOf<String>()
    private val defaultBaudRates = listOf(
        1200, 2400, 4800, 9600, 14400, 19200, 28800, 38400,
        57600, 76800, 115200, 153600, 230400, 307200, 460800, 921600
    )
    private val baudOptions = mutableListOf<BaudOption>()
    private lateinit var baudAdapter: ArrayAdapter<String>
    private var lastBaudSelection = 0
    private var ignoreBaudSelection = false
    private val preferences by lazy {
        getSharedPreferences("serial_prefs", Context.MODE_PRIVATE)
    }
    private val logItems = mutableListOf<SerialLog>()
    private lateinit var logAdapter: SerialLogAdapter
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val defaultBaud = 9600
    private var commonCommandsDialog: BottomSheetDialog? = null
    private var commonCommandsAdapter: CommonCommandAdapter? = null
    private var commonCommandsEmptyView: TextView? = null
    private var commonCommandsRecycler: RecyclerView? = null

    private val commonCommandsKey = "common_commands"
    private val commonCommandsAddedKey = "common_commands_added"
    private val exportDirName = "common_commands"
    private val exportFileName = "common_commands.json"
    private val importRequestCode = 1001

    private val dataListener = EasySerial.OnDataReceivedListener { _, data, length ->
        val content = if (isHexMode()) {
            formatHex(HexCodec.encode(data, 0, length))
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
        setContentView(R.layout.activity_main)
        mBtnConnect = findViewById<Button>(R.id.mBtnConnect)
        mBtnSend = findViewById<Button>(R.id.mBtnSend)
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
        mTvCommonCommands = findViewById<TextView>(R.id.mTvCommonCommands)
        autoScrollEnabled = mSwitchAutoScroll.isChecked
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
        if (currentPort.isNotEmpty()) {
            EasySerial.close(currentPort)
        }
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
                val openResult = EasySerial.open(selectedPort, baudRate)
                isConnecting = false
                if (openResult == EasySerial.OPEN_OK) {
                    currentPort = selectedPort
                    currentBaud = baudRate
                    isOpenSerial = true
                    lastConnectError = null
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
            } else {
                isOpenSerial = false
                if (currentPort.isNotEmpty()) {
                    EasySerial.close(currentPort)
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

        mRbHex.setOnCheckedChangeListener { _, _ ->
            refreshSendAvailability()
        }
        mRbAscii.setOnCheckedChangeListener { _, _ ->
            refreshSendAvailability()
        }

        mSwitchAutoScroll.setOnCheckedChangeListener { _, isChecked ->
            autoScrollEnabled = isChecked
        }
        mSwitchTime.setOnCheckedChangeListener { _, isChecked ->
            logAdapter.setShowTime(isChecked)
        }

        mTvCommonCommands.setOnClickListener {
            showCommonCommandsSheet()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == importRequestCode && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            importCommonCommands(uri)
        }
    }

    private fun loadSerialPorts() {
        val ports = SerialPortFinder().getAllDevicesPath().toList()
        portPaths.clear()
        hasPorts = ports.isNotEmpty()
        if (hasPorts) {
            portPaths.addAll(ports)
        } else {
            portPaths.add(resources.getString(R.string.text_no_ports))
        }
        val portAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            portPaths
        )
        portAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        mSpPort.adapter = portAdapter
        mBtnConnect.isEnabled = hasPorts
        mSpPort.isEnabled = hasPorts && !isOpenSerial
    }

    private fun updateConnectionUi() {
        if (isConnecting) {
            mTvStatus.text = resources.getString(R.string.text_status_connecting)
            mBtnConnect.text = resources.getString(R.string.text_connecting)
            mBtnConnect.isEnabled = false
        } else if (!hasPorts) {
            mTvStatus.text = resources.getString(R.string.text_status_no_ports)
            mBtnConnect.text = resources.getString(R.string.text_connect)
            mBtnConnect.isEnabled = false
        } else if (isOpenSerial && currentPort.isNotEmpty()) {
            mTvStatus.text = resources.getString(
                R.string.text_status_connected,
                currentPort,
                currentBaud
            )
            mBtnConnect.text = resources.getString(R.string.text_disconnect)
            mBtnConnect.isEnabled = true
        } else if (lastConnectError != null) {
            mTvStatus.text = resources.getString(
                R.string.text_status_failed,
                lastConnectError
            )
            mBtnConnect.text = resources.getString(R.string.text_connect)
            mBtnConnect.isEnabled = true
        } else {
            mTvStatus.text = resources.getString(R.string.text_status_idle)
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
        val input = EditText(this).apply {
            hint = resources.getString(R.string.text_rate_input)
            inputType = InputType.TYPE_CLASS_NUMBER
            keyListener = DigitsKeyListener.getInstance("0123456789")
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(9))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.text_baud_custom))
            .setView(input)
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
        preferences.edit().putInt("last_success_baud", baud).apply()
    }

    private fun loadLastSuccessfulBaud(): Int? {
        val value = preferences.getInt("last_success_baud", -1)
        return if (value > 0) value else null
    }

    private fun isValidHex(input: String): Boolean {
        val hex = normalizeHex(input)
        if (hex.isEmpty()) return false
        if (hex.length % 2 != 0) return false
        for (c in hex) {
            val ok = (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
            if (!ok) return false
        }
        return true
    }

    private fun isValidSendInput(): Boolean {
        val input = mEtInput.text.toString()
        val valid = if (input.isBlank()) {
            false
        } else {
            if (isHexMode()) {
                isValidHex(input)
            } else {
                true
            }
        }
        if (isHexMode() && input.isNotBlank() && !isValidHex(input)) {
            mEtInput.error = resources.getString(R.string.text_hex_invalid)
        } else {
            mEtInput.error = null
        }
        return valid
    }

    private fun normalizeHex(input: String): String {
        return input.replace("\\s".toRegex(), "")
    }

    private fun formatHex(hex: String): String {
        val normalized = normalizeHex(hex)
        return normalized.chunked(2).joinToString(" ")
    }

    private fun addLog(direction: Direction, content: String) {
        val log = SerialLog(timeFormat.format(Date()), direction, content)
        runOnUiThread {
            logAdapter.add(log)
            if (autoScrollEnabled) {
                mRvLogs.scrollToPosition(logAdapter.itemCount - 1)
            }
            updateEmptyView()
        }
    }

    private fun formatLogLine(log: SerialLog): String {
        val arrow = if (log.direction == Direction.TX) "▶" else "◀"
        val dirText = if (log.direction == Direction.TX) "TX" else "RX"
        return "[${log.time}] $arrow $dirText: ${log.content}"
    }

    private fun copyLogToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("serial-log", text))
        Toast.makeText(this, resources.getString(R.string.text_copy_success), Toast.LENGTH_SHORT)
            .show()
    }

    private fun showLogActionSheet(log: SerialLog) {
        val dialog = BottomSheetDialog(this)
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
                sendCommand(log.content, false)
            }
            dialog.dismiss()
        }
        tvCopy.setOnClickListener {
            copyLogToClipboard(log.content)
            dialog.dismiss()
        }
        tvAddCommon.setOnClickListener {
            if (isTx) {
                addToCommonCommands(log.content)
            }
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showCommonCommandsSheet() {
        if (commonCommandsDialog == null) {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_common_commands, null)
            val tvExport = view.findViewById<TextView>(R.id.mTvExport)
            val tvImport = view.findViewById<TextView>(R.id.mTvImport)
            val recycler = view.findViewById<RecyclerView>(R.id.mRvCommonCommands)
            val emptyView = view.findViewById<TextView>(R.id.mTvCommonEmpty)
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.addItemDecoration(
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
            )
            val adapter = CommonCommandAdapter(
                mutableListOf(),
                onClick = { command ->
                    sendCommand(command, false)
                    dialog.dismiss()
                },
                onDelete = { command ->
                    removeCommonCommand(command)
                }
            )
            recycler.adapter = adapter
            tvExport.setOnClickListener { exportCommonCommands() }
            tvImport.setOnClickListener { launchImportPicker() }
            dialog.setContentView(view)
            commonCommandsDialog = dialog
            commonCommandsAdapter = adapter
            commonCommandsEmptyView = emptyView
            commonCommandsRecycler = recycler
        }
        updateCommonCommandsList()
        commonCommandsDialog?.show()
    }

    private fun updateCommonCommandsList() {
        val commands = loadCommonCommands()
        commonCommandsAdapter?.setItems(commands)
        val showEmpty = commands.isEmpty() && !hasEverAddedCommon()
        commonCommandsEmptyView?.visibility = if (showEmpty) View.VISIBLE else View.GONE
        commonCommandsRecycler?.visibility = if (showEmpty) View.GONE else View.VISIBLE
    }

    private fun loadCommonCommands(): MutableList<String> {
        val raw = preferences.getString(commonCommandsKey, "[]") ?: "[]"
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotBlank()) {
                    result.add(value)
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun saveCommonCommands(commands: List<String>) {
        val array = JSONArray()
        commands.forEach { array.put(it) }
        preferences.edit().putString(commonCommandsKey, array.toString()).apply()
    }

    private fun hasEverAddedCommon(): Boolean {
        return preferences.getBoolean(commonCommandsAddedKey, false)
    }

    private fun markEverAddedCommon() {
        preferences.edit().putBoolean(commonCommandsAddedKey, true).apply()
    }

    private fun addToCommonCommands(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        val commands = loadCommonCommands()
        if (commands.contains(trimmed)) {
            Toast.makeText(this, resources.getString(R.string.text_common_exists), Toast.LENGTH_SHORT)
                .show()
            return
        }
        commands.add(0, trimmed)
        saveCommonCommands(commands)
        markEverAddedCommon()
        updateCommonCommandsList()
        Toast.makeText(this, resources.getString(R.string.text_common_added), Toast.LENGTH_SHORT)
            .show()
    }

    private fun removeCommonCommand(command: String) {
        val commands = loadCommonCommands()
        if (commands.remove(command)) {
            saveCommonCommands(commands)
            updateCommonCommandsList()
            Toast.makeText(this, resources.getString(R.string.text_common_deleted), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun exportCommonCommands() {
        val commands = loadCommonCommands()
        val exportDir = File(Environment.getExternalStorageDirectory(), exportDirName)
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Toast.makeText(this, resources.getString(R.string.text_export_failed), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val file = File(exportDir, exportFileName)
        val array = JSONArray()
        commands.forEach { array.put(it) }
        try {
            file.writeText(array.toString(), Charset.forName("UTF-8"))
            Toast.makeText(
                this,
                resources.getString(R.string.text_export_done, commands.size),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: IOException) {
            Toast.makeText(this, resources.getString(R.string.text_export_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun launchImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
        }
        startActivityForResult(intent, importRequestCode)
    }

    private fun importCommonCommands(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val content = inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
            val imported = mutableListOf<String>()
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotBlank()) {
                    imported.add(value)
                }
            }
            val existing = loadCommonCommands()
            val existingSet = existing.toMutableSet()
            var addedCount = 0
            val uniqueImported = imported.distinct()
            for (i in uniqueImported.indices.reversed()) {
                val cmd = uniqueImported[i]
                if (existingSet.add(cmd)) {
                    existing.add(0, cmd)
                    addedCount++
                }
            }
            val skipped = uniqueImported.size - addedCount
            if (addedCount > 0) {
                saveCommonCommands(existing)
                markEverAddedCommon()
            }
            updateCommonCommandsList()
            Toast.makeText(
                this,
                resources.getString(R.string.text_import_done, addedCount, skipped),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(this, resources.getString(R.string.text_import_failed), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun sendCommand(content: String, showInputError: Boolean) {
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
        if (isHexMode() && !isValidHex(content)) {
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
                    val hex = normalizeHex(content)
                    EasySerial.send(currentPort, HexCodec.decode(hex))
                    addLog(Direction.TX, formatHex(hex))
                } else {
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    EasySerial.send(currentPort, bytes)
                    addLog(Direction.TX, content)
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
