package com.example.timesheet.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime

object FirebaseRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private const val EMPLOYEES = "employees"
    private const val ORGANIZATIONS = "organizations"
    private const val ENTRIES = "entries"
    private const val SETTINGS = "settings"

    fun observeEmployees(): Flow<List<Employee>> = callbackFlow {
        val reg = db.collection(EMPLOYEES).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            val list = snap?.documents?.map { doc ->
                Employee(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    hourlyRate = doc.getDouble("hourlyRate") ?: 0.0
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeOrganizations(): Flow<List<Organization>> = callbackFlow {
        val reg = db.collection(ORGANIZATIONS).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            val list = snap?.documents?.map { doc ->
                Organization(id = doc.id, name = doc.getString("name") ?: "")
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeEntries(): Flow<List<LedgerEntry>> = callbackFlow {
        val reg = db.collection(ENTRIES).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            val list = snap?.documents?.mapNotNull { doc ->
                runCatching {
                    LedgerEntry(
                        id = doc.id,
                        date = LocalDate.parse(doc.getString("date")!!),
                        startTime = doc.getString("startTime")?.let { LocalTime.parse(it) },
                        endTime = doc.getString("endTime")?.let { LocalTime.parse(it) },
                        shiftType = doc.getString("shiftType")?.let { name ->
                            runCatching { ShiftType.valueOf(name) }.getOrNull()
                        } ?: ShiftType.DAY,
                        type = EntryType.valueOf(doc.getString("type") ?: EntryType.SHIFT.name),
                        employeeId = doc.getString("employeeId"),
                        organizationId = doc.getString("organizationId"),
                        amount = doc.getDouble("amount") ?: 0.0,
                        hours = doc.getDouble("hours") ?: 0.0,
                        note = doc.getString("note") ?: "",
                        expenseCategoryId = doc.getString("expenseCategoryId"),
                        unitId = doc.getString("unitId"),
                        quantity = doc.getDouble("quantity") ?: 0.0,
                        surchargeIds = (doc.get("surchargeIds") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList(),
                        unpaidBreakMinutes = (doc.getLong("unpaidBreakMinutes") ?: 0L).toInt(),
                        overtimeEnabled = doc.getBoolean("overtimeEnabled") ?: false,
                        projectName = doc.getString("projectName") ?: "",
                        adjustmentTypeName = doc.getString("adjustmentTypeName") ?: "",
                        timeTypeId = doc.getString("timeTypeId"),
                        attachments = (doc.get("attachments") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList()
                    )
                }.getOrNull()
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun upsertEmployee(e: Employee) {
        db.collection(EMPLOYEES).document(e.id)
            .set(mapOf("name" to e.name, "hourlyRate" to e.hourlyRate)).await()
    }

    suspend fun deleteEmployee(id: String) {
        db.collection(EMPLOYEES).document(id).delete().await()
    }

    suspend fun upsertOrganization(o: Organization) {
        db.collection(ORGANIZATIONS).document(o.id)
            .set(mapOf("name" to o.name)).await()
    }

    suspend fun deleteOrganization(id: String) {
        db.collection(ORGANIZATIONS).document(id).delete().await()
    }

    suspend fun upsertEntry(entry: LedgerEntry) {
        db.collection(ENTRIES).document(entry.id).set(
            mapOf(
                "date" to entry.date.toString(),
                "startTime" to entry.startTime?.toString(),
                "endTime" to entry.endTime?.toString(),
                "shiftType" to entry.shiftType.name,
                "type" to entry.type.name,
                "employeeId" to entry.employeeId,
                "organizationId" to entry.organizationId,
                "amount" to entry.amount,
                "hours" to entry.hours,
                "note" to entry.note,
                "expenseCategoryId" to entry.expenseCategoryId,
                "unitId" to entry.unitId,
                "quantity" to entry.quantity,
                "surchargeIds" to entry.surchargeIds,
                "unpaidBreakMinutes" to entry.unpaidBreakMinutes,
                "overtimeEnabled" to entry.overtimeEnabled,
                "projectName" to entry.projectName,
                "adjustmentTypeName" to entry.adjustmentTypeName,
                "timeTypeId" to entry.timeTypeId,
                "attachments" to entry.attachments
            )
        ).await()
    }

    suspend fun deleteEntry(id: String) {
        db.collection(ENTRIES).document(id).delete().await()
    }

    suspend fun pushOpeningBalance(value: Double) {
        db.collection(SETTINGS).document("shared")
            .set(mapOf("openingBalance" to value)).await()
    }
}