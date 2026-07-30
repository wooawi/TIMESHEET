package com.example.timesheet.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BackupFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val createdAtMillis: Long
)


object BackupManager {

    private fun backupsDir(context: Context): File =
        File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }

    fun listBackups(context: Context): List<BackupFile> =
        backupsDir(context).listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { BackupFile(it, it.nameWithoutExtension, it.length(), it.lastModified()) }
            ?: emptyList()

    fun createBackup(
        context: Context,
        state: AppState,
        settings: JSONObject,
        label: String? = null
    ): File {
        val root = JSONObject()
        root.put("version", 1)
        root.put("state", stateToJson(state))
        root.put("settings", settings)

        val safeLabel = (label?.takeIf { it.isNotBlank() } ?: "Зеленый ТабельBackup")
            .replace(Regex("[^A-Za-zА-Яа-я0-9 _-]"), "")
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val file = File(backupsDir(context), "${safeLabel}_$timestamp.json")
        file.writeText(root.toString(2))
        return file
    }

    fun restoreBackup(file: File): Pair<AppState, JSONObject> {
        val root = JSONObject(file.readText())
        val state = jsonToState(root.getJSONObject("state"))
        val settings = root.optJSONObject("settings") ?: JSONObject()
        return state to settings
    }

    fun deleteBackup(file: File) = file.delete()


    fun stateToJson(state: AppState): JSONObject {
        val root = JSONObject()
        root.put("openingBalance", state.openingBalance)

        val employees = JSONArray()
        state.employees.forEach {
            employees.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("hourlyRate", it.hourlyRate)
            })
        }
        root.put("employees", employees)

        val organizations = JSONArray()
        state.organizations.forEach {
            organizations.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
            })
        }
        root.put("organizations", organizations)

        val entries = JSONArray()
        state.entries.forEach {
            entries.put(JSONObject().apply {
                put("id", it.id)
                put("date", it.date.toString())
                put("type", it.type.name)
                put("employeeId", it.employeeId ?: JSONObject.NULL)
                put("organizationId", it.organizationId ?: JSONObject.NULL)
                put("amount", it.amount)
                put("hours", it.hours)
                put("note", it.note)
            })
        }
        root.put("entries", entries)
        return root
    }

    fun jsonToState(root: JSONObject): AppState {
        val employees = mutableListOf<Employee>()
        root.optJSONArray("employees")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                employees.add(
                    Employee(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        hourlyRate = o.optDouble("hourlyRate", 0.0)
                    )
                )
            }
        }

        val organizations = mutableListOf<Organization>()
        root.optJSONArray("organizations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                organizations.add(Organization(id = o.getString("id"), name = o.getString("name")))
            }
        }

        val entries = mutableListOf<LedgerEntry>()
        root.optJSONArray("entries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                entries.add(
                    LedgerEntry(
                        id = o.getString("id"),
                        date = LocalDate.parse(o.getString("date")),
                        type = EntryType.valueOf(o.getString("type")),
                        employeeId = if (o.isNull("employeeId")) null else o.getString("employeeId"),
                        organizationId = if (o.isNull("organizationId")) null else o.getString("organizationId"),
                        amount = o.optDouble("amount", 0.0),
                        hours = o.optDouble("hours", 0.0),
                        note = o.optString("note", "")
                    )
                )
            }
        }

        return AppState(
            employees = employees,
            organizations = organizations,
            entries = entries,
            openingBalance = root.optDouble("openingBalance", 0.0)
        )
    }
}
