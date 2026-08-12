package com.example.timesheet.data

import java.time.LocalDate
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

    /**
     * Сохранён старый метод (расчёт за календарный месяц), используется на главном экране.
     * ИЗМЕНЕНО: теперь принимает справочник доплат, чтобы они реально участвовали в начислении
     * (раньше Surcharge/ShiftTemplate.surchargeIds нигде не влияли на сумму — это и была
     * основная ошибка в математике).
     */
    fun calculate(
        entries: List<LedgerEntry>,
        employees: List<Employee>,
        month: YearMonth,
        employeeFilter: String?,
        organizationFilter: String?,
        openingBalance: Double,
        surcharges: List<Surcharge> = emptyList()
    ): PayrollBreakdown {
        return calculateForPeriod(
            entries = entries,
            employees = employees,
            surcharges = surcharges,
            start = month.atDay(1),
            end = month.atEndOfMonth(),
            employeeFilter = employeeFilter,
            organizationFilter = organizationFilter,
            openingBalance = openingBalance
        )
    }

    /**
     * ДОБАВЛЕНО (ТЗ): расчёт за произвольный период — используется в «Настроить период»,
     * «Журнал смен» и во всех трёх отчётах («Расчетный лист», «Рабочее время по периодам»,
     * «Расходы»), а не только за календарный месяц, как было раньше.
     */
    fun calculateForPeriod(
        entries: List<LedgerEntry>,
        employees: List<Employee>,
        surcharges: List<Surcharge>,
        start: LocalDate,
        end: LocalDate,
        employeeFilter: String?,
        organizationFilter: String?,
        openingBalance: Double
    ): PayrollBreakdown {
        val filtered = entries.filter { entry ->
            !entry.date.isBefore(start) && !entry.date.isAfter(end) &&
                    (employeeFilter == null || entry.employeeId == employeeFilter) &&
                    (organizationFilter == null || entry.organizationId == organizationFilter)
        }

        val rateByEmployee = employees.associateBy({ it.id }, { it.hourlyRate })
        val surchargeById = surcharges.associateBy { it.id }

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
                    val base = hours * rate * multiplier

                    // Доплаты/удержания, привязанные к смене (см. Models.kt: LedgerEntry.surchargeIds)
                    var shiftTotal = base
                    entry.surchargeIds.forEach surchargeLoop@{ surchargeId ->
                        val surcharge = surchargeById[surchargeId] ?: return@surchargeLoop
                        val weekdayOk = surcharge.activeWeekdays.isEmpty() ||
                                surcharge.activeWeekdays.contains(entry.date.dayOfWeek.value)
                        if (!weekdayOk) return@surchargeLoop

                        val contribution = when (surcharge.calcType) {
                            SurchargeCalcType.FIXED_PER_SHIFT -> surcharge.amount
                            SurchargeCalcType.PER_HOUR -> surcharge.amount * hours
                            SurchargeCalcType.PERCENT -> base * (surcharge.amount / 100.0)
                        }
                        shiftTotal += if (surcharge.kind == SurchargeKind.BONUS) contribution else -contribution
                    }

                    accrued += shiftTotal
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