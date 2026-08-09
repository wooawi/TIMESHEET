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
                put("expenseCategoryId", it.expenseCategoryId ?: JSONObject.NULL)
                put("unitId", it.unitId ?: JSONObject.NULL)
                put("quantity", it.quantity)
            })
        }
        root.put("entries", entries)

        val surcharges = JSONArray()
        state.surcharges.forEach {
            surcharges.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("kind", it.kind.name)
                put("calcType", it.calcType.name)
                put("amount", it.amount)
                put("taxable", it.taxable)
                put("activeWeekdays", JSONArray(it.activeWeekdays.toList()))
                put("isBuiltIn", it.isBuiltIn)
            })
        }
        root.put("surcharges", surcharges)

        val shiftTemplates = JSONArray()
        state.shiftTemplates.forEach {
            shiftTemplates.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("employeeId", it.employeeId ?: JSONObject.NULL)
                put("organizationId", it.organizationId ?: JSONObject.NULL)
                put("shiftTypeName", it.shiftTypeName)
                put("projectName", it.projectName)
                put("surchargeIds", JSONArray(it.surchargeIds))
                put("comment", it.comment)
            })
        }
        root.put("shiftTemplates", shiftTemplates)

        // Новые справочники
        val timeTypes = JSONArray()
        state.timeTypes.forEach {
            timeTypes.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("code", it.code)
                put("color", it.color)
                put("isBuiltIn", it.isBuiltIn)
            })
        }
        root.put("timeTypes", timeTypes)

        val expenseCategories = JSONArray()
        state.expenseCategories.forEach {
            expenseCategories.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("isBuiltIn", it.isBuiltIn)
            })
        }
        root.put("expenseCategories", expenseCategories)

        val units = JSONArray()
        state.units.forEach {
            units.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("shortName", it.shortName)
                put("isBuiltIn", it.isBuiltIn)
            })
        }
        root.put("units", units)

        val taxes = JSONArray()
        state.taxes.forEach {
            taxes.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("rate", it.rate)
                put("isBuiltIn", it.isBuiltIn)
            })
        }
        root.put("taxes", taxes)

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
                        note = o.optString("note", ""),
                        expenseCategoryId = if (o.isNull("expenseCategoryId")) null else o.getString("expenseCategoryId"),
                        unitId = if (o.isNull("unitId")) null else o.getString("unitId"),
                        quantity = o.optDouble("quantity", 0.0)
                    )
                )
            }
        }

        val surcharges = mutableListOf<Surcharge>()
        root.optJSONArray("surcharges")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val weekdays = mutableSetOf<Int>()
                o.optJSONArray("activeWeekdays")?.let { wd ->
                    for (j in 0 until wd.length()) weekdays.add(wd.getInt(j))
                }
                surcharges.add(
                    Surcharge(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        kind = runCatching { SurchargeKind.valueOf(o.getString("kind")) }.getOrDefault(SurchargeKind.BONUS),
                        calcType = runCatching { SurchargeCalcType.valueOf(o.getString("calcType")) }.getOrDefault(SurchargeCalcType.FIXED_PER_SHIFT),
                        amount = o.optDouble("amount", 0.0),
                        taxable = o.optBoolean("taxable", true),
                        activeWeekdays = weekdays,
                        isBuiltIn = o.optBoolean("isBuiltIn", false)
                    )
                )
            }
        }

        val shiftTemplates = mutableListOf<ShiftTemplate>()
        root.optJSONArray("shiftTemplates")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val surchargeIds = mutableListOf<String>()
                o.optJSONArray("surchargeIds")?.let { ids ->
                    for (j in 0 until ids.length()) surchargeIds.add(ids.getString(j))
                }
                shiftTemplates.add(
                    ShiftTemplate(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        employeeId = if (o.isNull("employeeId")) null else o.getString("employeeId"),
                        organizationId = if (o.isNull("organizationId")) null else o.getString("organizationId"),
                        shiftTypeName = o.optString("shiftTypeName", ""),
                        projectName = o.optString("projectName", ""),
                        surchargeIds = surchargeIds,
                        comment = o.optString("comment", "")
                    )
                )
            }
        }

        // Новые справочники
        val timeTypes = mutableListOf<TimeType>()
        root.optJSONArray("timeTypes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                timeTypes.add(
                    TimeType(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        code = o.optString("code", ""),
                        color = o.optString("color", "#45B7D1"),
                        isBuiltIn = o.optBoolean("isBuiltIn", false)
                    )
                )
            }
        }

        val expenseCategories = mutableListOf<ExpenseCategory>()
        root.optJSONArray("expenseCategories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                expenseCategories.add(
                    ExpenseCategory(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        isBuiltIn = o.optBoolean("isBuiltIn", false)
                    )
                )
            }
        }

        val units = mutableListOf<UnitOfMeasure>()
        root.optJSONArray("units")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                units.add(
                    UnitOfMeasure(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        shortName = o.optString("shortName", ""),
                        isBuiltIn = o.optBoolean("isBuiltIn", true)
                    )
                )
            }
        }

        val taxes = mutableListOf<Tax>()
        root.optJSONArray("taxes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                taxes.add(
                    Tax(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        rate = o.optDouble("rate", 0.0),
                        isBuiltIn = o.optBoolean("isBuiltIn", false)
                    )
                )
            }
        }

        return AppState(
            employees = employees,
            organizations = organizations,
            entries = entries,
            openingBalance = root.optDouble("openingBalance", 0.0),
            surcharges = surcharges,
            shiftTemplates = shiftTemplates,
            timeTypes = timeTypes,
            expenseCategories = expenseCategories,
            units = units,
            taxes = taxes
        )
    }
}