package com.example.timesheet.data

import java.time.LocalDate
import java.util.UUID

data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val hourlyRate: Double = 0.0
)

data class Organization(
    val id: String = UUID.randomUUID().toString(),
    val name: String = ""
)

enum class EntryType {
    SHIFT,
    PAYMENT,
    TAX,
    ADJUSTMENT
}

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val type: EntryType = EntryType.SHIFT,
    val employeeId: String? = null,
    val organizationId: String? = null,
    val amount: Double = 0.0,
    val hours: Double = 0.0,
    val note: String = ""
)

data class AppState(
    val employees: List<Employee> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val entries: List<LedgerEntry> = emptyList(),
    val openingBalance: Double = 0.0
)
