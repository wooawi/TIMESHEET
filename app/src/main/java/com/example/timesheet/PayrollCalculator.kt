package com.example.timesheet.data

import java.time.YearMonth

data class PayrollBreakdown(
    val accrued: Double,
    val openingBalance: Double,
    val payments: Double,
    val taxes: Double,
    val adjustments: Double,
    val expenses: Double,
    val closingBalance: Double,
    val entries: List<LedgerEntry> = emptyList(),
    val totalHours: Double = 0.0
)

object PayrollCalculator {

    fun calculate(
        entries: List<LedgerEntry>,
        employees: List<Employee>,
        month: YearMonth,
        employeeFilter: String?,
        organizationFilter: String?,
        openingBalance: Double
    ): PayrollBreakdown {
        val filtered = entries.filter { entry ->
            YearMonth.from(entry.date) == month &&
                    (employeeFilter == null || entry.employeeId == employeeFilter) &&
                    (organizationFilter == null || entry.organizationId == organizationFilter)
        }

        val rateByEmployee = employees.associateBy({ it.id }, { it.hourlyRate })

        var accrued = 0.0
        var payments = 0.0
        var taxes = 0.0
        var adjustments = 0.0
        var expenses = 0.0
        var totalHours = 0.0

        filtered.forEach { entry ->
            when (entry.type) {
                EntryType.SHIFT -> {
                    val hours = entry.calculateHours()
                    totalHours += hours
                    val rate = entry.employeeId?.let { rateByEmployee[it] } ?: 0.0
                    val multiplier = when (entry.shiftType) {
                        ShiftType.DAY -> 1.0
                        ShiftType.NIGHT -> 1.4
                        ShiftType.HOLIDAY -> 2.0
                        ShiftType.OVERTIME -> 1.5
                        ShiftType.WEEKEND -> 1.5
                    }
                    accrued += hours * rate * multiplier
                }
                EntryType.PAYMENT -> payments += entry.amount
                EntryType.TAX -> taxes += entry.amount
                EntryType.ADJUSTMENT -> adjustments += entry.amount
                EntryType.EXPENSE -> expenses += entry.amount
            }
        }

        val total = accrued - payments - taxes + adjustments - expenses
        val closing = openingBalance + total

        return PayrollBreakdown(
            accrued = accrued,
            openingBalance = openingBalance,
            payments = payments,
            taxes = taxes,
            adjustments = adjustments,
            expenses = expenses,
            closingBalance = closing,
            entries = filtered,
            totalHours = totalHours
        )
    }
}