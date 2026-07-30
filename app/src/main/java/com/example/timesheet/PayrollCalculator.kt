package com.example.timesheet.data

import java.time.YearMonth

data class PayrollBreakdown(
    val accrued: Double,
    val openingBalance: Double,
    val payments: Double,
    val closingBalance: Double
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

        filtered.forEach { entry ->
            when (entry.type) {
                EntryType.SHIFT -> {
                    val rate = entry.employeeId?.let { rateByEmployee[it] } ?: 0.0
                    accrued += entry.hours * rate
                }
                EntryType.ADJUSTMENT -> accrued += entry.amount
                EntryType.TAX -> accrued -= entry.amount
                EntryType.PAYMENT -> payments += entry.amount
            }
        }

        val closing = openingBalance + accrued - payments
        return PayrollBreakdown(
            accrued = accrued,
            openingBalance = openingBalance,
            payments = payments,
            closingBalance = closing
        )
    }
}
