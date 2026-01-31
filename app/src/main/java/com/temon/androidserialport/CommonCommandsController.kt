package com.temon.androidserialport

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

class CommonCommandsController(
    private val activity: Activity,
    private val serialPreferences: SerialPreferences,
    private val onSendCommand: (CommonCommand) -> Unit
) {

    val importRequestCode: Int = 1001
    val exportPermissionRequestCode: Int = 2001
    private val pickDirectoryRequestCode: Int = 1002

    private val exportFileName = "common_commands.txt"

    private var dialog: BottomSheetDialog? = null
    private var adapter: CommonCommandAdapter? = null
    private var emptyView: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var contentContainer: View? = null
    private var exportView: TextView? = null
    private var clearAllView: TextView? = null

    private val commandsAssetDir = "common_commands"

    fun show() {
        if (dialog == null) {
            buildDialog()
        }
        updateList()
        dialog?.show()
    }

    fun addCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        val commands = loadCommands()
        if (commands.any { it.content == trimmed }) {
            Toast.makeText(
                activity, activity.getString(R.string.text_common_exists), Toast.LENGTH_SHORT
            ).show()
            return
        }
        commands.add(0, CommonCommand(content = trimmed))
        saveCommands(commands)
        markEverAddedCommon()
        updateList()
        Toast.makeText(activity, activity.getString(R.string.text_common_added), Toast.LENGTH_SHORT)
            .show()
    }

    fun removeCommand(command: CommonCommand) {
        val commands = loadCommands()
        val removed = commands.removeAll { it.content == command.content }
        if (removed) {
            saveCommands(commands)
            updateList()
            Toast.makeText(
                activity, activity.getString(R.string.text_common_deleted), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun launchImportPicker(initialDir: Uri? = null) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain")
            )
            if (initialDir != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialDir)
            }
        }
        activity.startActivityForResult(intent, importRequestCode)
    }

    private fun launchDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(intent, pickDirectoryRequestCode)
    }

    fun handleImportResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != importRequestCode && requestCode != pickDirectoryRequestCode) {
            return false
        }
        if (resultCode != Activity.RESULT_OK) return true
        return when (requestCode) {
            pickDirectoryRequestCode -> {
                val dirUri = data?.data
                launchImportPicker(dirUri)
                true
            }

            else -> {
                val uri = data?.data ?: return true
                importCommonCommands(uri)
                true
            }
        }
    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != exportPermissionRequestCode) return false
        val granted =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            exportCommonCommandsToDownloadsInternal()
        } else {
            Toast.makeText(
                activity, activity.getString(R.string.text_export_failed), Toast.LENGTH_SHORT
            ).show()
        }
        return true
    }

    private fun buildDialog() {
        val sheet = BottomSheetDialog(activity, R.style.AppBottomSheetDialogTheme)
        val view = activity.layoutInflater.inflate(R.layout.bottom_sheet_common_commands, null)
        val tvExport = view.findViewById<TextView>(R.id.mTvExport)
        val tvClearAll = view.findViewById<TextView>(R.id.mTvClearAll)
        val tvImport = view.findViewById<TextView>(R.id.mTvImport)
        val container = view.findViewById<View>(R.id.mCommonContentContainer)
        val recycler = view.findViewById<RecyclerView>(R.id.mRvCommonCommands)
        val empty = view.findViewById<TextView>(R.id.mTvCommonEmpty)
        applyBottomSheetMaxHeight(container)
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.addItemDecoration(
            DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        )
        val cmdAdapter = CommonCommandAdapter(mutableListOf(), onClick = { command ->
            onSendCommand(command)
            sheet.dismiss()
        }, onEdit = { command ->
            showEditDialog(command)
        }, onDelete = { command ->
            removeCommand(command)
        })
        recycler.adapter = cmdAdapter
        tvExport.setOnClickListener {
            exportCommonCommandsToDownloads()
            sheet.dismiss()
        }
        tvImport.setOnClickListener {
            launchDirectoryPicker()
            sheet.dismiss()
        }
        tvImport.setOnLongClickListener {
            showBuiltinAssetPicker(sheet)
            true
        }
        tvClearAll.setOnClickListener {
            showClearAllDialog()
        }
        sheet.setContentView(view)
        sheet.setOnShowListener { dialogInterface ->
            val bottomSheet =
                (dialogInterface as? BottomSheetDialog)?.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
            bottomSheet.post {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        dialog = sheet
        adapter = cmdAdapter
        emptyView = empty
        recyclerView = recycler
        contentContainer = container
        exportView = tvExport
        clearAllView = tvClearAll
    }

    private fun applyBottomSheetMaxHeight(vararg targets: View) {
        val maxHeight =
            (activity.resources.displayMetrics.heightPixels * 0.65f).toInt()
        targets.forEach { target ->
            val params = target.layoutParams
            if (params is ConstraintLayout.LayoutParams) {
                params.height = maxHeight
                target.layoutParams = params
            } else {
                params.height = maxHeight
                target.layoutParams = params
            }
        }
    }

    private fun updateList() {
        val commands = loadCommands()
        adapter?.setItems(commands)
        val showEmpty = commands.isEmpty()
        emptyView?.visibility = if (showEmpty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (showEmpty) View.GONE else View.VISIBLE
        val hasCommands = commands.isNotEmpty()
        exportView?.isEnabled = hasCommands
        exportView?.alpha = if (hasCommands) 1f else 0.5f
        clearAllView?.isEnabled = hasCommands
        clearAllView?.alpha = if (hasCommands) 1f else 0.5f
    }

    private fun showClearAllDialog() {
        if (loadCommands().isEmpty()) return
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.text_clear_common_commands))
            .setMessage(activity.getString(R.string.text_clear_common_confirm))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                saveCommands(emptyList())
                updateList()
                Toast.makeText(
                    activity,
                    activity.getString(R.string.text_common_cleared),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showBuiltinAssetPicker(sheet: BottomSheetDialog) {
        val assetFiles = try {
            activity.assets.list(commandsAssetDir)?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        val commandFiles = assetFiles.filter {
            it.endsWith(".txt", ignoreCase = true) || it.endsWith(".json", ignoreCase = true)
        }
        if (commandFiles.isEmpty()) {
            Toast.makeText(
                activity, activity.getString(R.string.text_import_failed), Toast.LENGTH_SHORT
            ).show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.text_import_builtin_commands))
            .setItems(commandFiles.toTypedArray()) { _, which ->
                importCommandsFromAssets(commandFiles[which])
                sheet.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun importCommandsFromAssets(assetName: String) {
        try {
            val content = activity.assets.open("$commandsAssetDir/$assetName")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }
            importCommonCommandsFromContent(content)
        } catch (_: Exception) {
            Toast.makeText(
                activity, activity.getString(R.string.text_import_failed), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showEditDialog(command: CommonCommand) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_edit_common_command, null)
        val etTitle = view.findViewById<EditText>(R.id.mEtCommandTitle)
        val etContent = view.findViewById<EditText>(R.id.mEtCommandContent)
        etTitle.setText(command.title.orEmpty())
        etContent.setText(command.content)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.text_edit_common_command)).setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newContent = etContent.text.toString().trim()
                if (newContent.isBlank()) {
                    etContent.error = activity.getString(R.string.text_command_content_required)
                    return@setOnClickListener
                }
                val newTitle = etTitle.text.toString().trim().takeIf { it.isNotBlank() }
                val commands = loadCommands()
                val index = commands.indexOfFirst { it.content == command.content }
                if (index == -1) {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                if (newContent != command.content && commands.any { it.content == newContent }) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.text_common_exists),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                commands[index] = CommonCommand(title = newTitle, content = newContent)
                saveCommands(commands)
                updateList()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun loadCommands(): MutableList<CommonCommand> {
        val raw = serialPreferences.getCommonCommandsJson()
        val result = mutableListOf<CommonCommand>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val entry = array.opt(i)
                when (entry) {
                    is JSONObject -> {
                        val content =
                            entry.optString("content", entry.optString("command", "")).trim()
                        val title = entry.optString("title", "").trim().takeIf { it.isNotBlank() }
                        if (content.isNotBlank()) {
                            result.add(CommonCommand(title = title, content = content))
                        }
                    }

                    is String -> {
                        val value = entry.trim()
                        if (value.isNotBlank()) {
                            result.add(CommonCommand(content = value))
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun saveCommands(commands: List<CommonCommand>) {
        val array = JSONArray()
        commands.forEach { command ->
            val obj = JSONObject()
            obj.put("content", command.content)
            command.title?.trim()?.takeIf { it.isNotBlank() }?.let { obj.put("title", it) }
            array.put(obj)
        }
        serialPreferences.setCommonCommandsJson(array.toString())
    }

    private fun markEverAddedCommon() {
        serialPreferences.setCommonCommandsAdded(true)
    }

    private fun exportCommonCommandsToDownloads() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            val granted = ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(permission), exportPermissionRequestCode
                )
                return
            }
        }
        exportCommonCommandsToDownloadsInternal()
    }

    private fun exportCommonCommandsToDownloadsInternal() {
        val commands = loadCommands()
        val array = JSONArray()
        commands.forEach { command ->
            val obj = JSONObject()
            obj.put("content", command.content)
            command.title?.trim()?.takeIf { it.isNotBlank() }?.let { obj.put("title", it) }
            array.put(obj)
        }
        try {
            val data = array.toString().toByteArray(Charset.forName("UTF-8"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = activity.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, exportFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("export uri unavailable")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        output.write(data)
                        output.flush()
                    } ?: throw IOException("export stream unavailable")
                } catch (e: IOException) {
                    resolver.delete(uri, null, null)
                    throw e
                }
            } else {
                val downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                    throw IOException("create download dir failed")
                }
                val file = File(downloadDir, exportFileName)
                file.writeBytes(data)
            }
            val dirLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${Environment.DIRECTORY_DOWNLOADS}/$exportFileName"
            } else {
                val downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                "${downloadDir.absolutePath}/${exportFileName}"
            }
            Toast.makeText(
                activity,
                "${activity.getString(R.string.text_export_done, commands.size)}，保存至：$dirLabel",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: IOException) {
            Toast.makeText(
                activity, activity.getString(R.string.text_export_failed), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun importCommonCommands(uri: Uri) {
        try {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return
            val content = inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
            importCommonCommandsFromContent(content)
        } catch (_: Exception) {
            Toast.makeText(
                activity, activity.getString(R.string.text_import_failed), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun importCommonCommandsFromContent(content: String) {
        val imported = mutableListOf<CommonCommand>()
        val array = JSONArray(content)
        for (i in 0 until array.length()) {
            val entry = array.opt(i)
            when (entry) {
                is JSONObject -> {
                    val contentValue =
                        entry.optString("content", entry.optString("command", "")).trim()
                    val title = entry.optString("title", "").trim().takeIf { it.isNotBlank() }
                    if (contentValue.isNotBlank()) {
                        imported.add(CommonCommand(title = title, content = contentValue))
                    }
                }

                is String -> {
                    val value = entry.trim()
                    if (value.isNotBlank()) {
                        imported.add(CommonCommand(content = value))
                    }
                }
            }
        }
        val existing = loadCommands()
        val existingSet = existing.map { it.content }.toMutableSet()
        var addedCount = 0
        val uniqueImported = imported.distinctBy { it.content }
        for (i in uniqueImported.indices.reversed()) {
            val cmd = uniqueImported[i]
            if (existingSet.add(cmd.content)) {
                existing.add(0, cmd)
                addedCount++
            }
        }
        val skipped = uniqueImported.size - addedCount
        if (addedCount > 0) {
            saveCommands(existing)
            markEverAddedCommon()
        }
        updateList()
        Toast.makeText(
            activity,
            activity.getString(R.string.text_import_done, addedCount, skipped),
            Toast.LENGTH_SHORT
        ).show()
    }
}
