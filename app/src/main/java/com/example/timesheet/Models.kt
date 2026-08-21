package com.example.timesheet.data

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// ========== ОСНОВНЫЕ МОДЕЛИ ==========
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
    ADJUSTMENT,
    EXPENSE
}

enum class ShiftType {
    DAY,
    NIGHT,
    HOLIDAY,
    OVERTIME,
    WEEKEND
}

/**
 * ИСПРАВЛЕНО (ТЗ: «сделать нормальную единую связь со всеми вкладками, у меня же
 * есть база данных», «почему я могу удалить из справочника дневную смену, а в
 * самой смене она останется», «почему когда я добавляю сверхурочную смену она
 * всё равно отображается как дневная»):
 *
 * Раньше выбор типа смены в диалоге «Смена» был жёстко зашитым списком из 5
 * значений enum `ShiftType` — он вообще не был связан со справочником «Типы
 * времени» (`AppViewModel.timeTypes`). Поэтому:
 *  - удаление записи из справочника никак не влияло на диалог смены (список
 *    оставался прежним, «зашитым» в код);
 *  - добавление своего типа в справочник никак не появлялось в диалоге смены
 *    (там просто не было механизма его показать);
 *  - при сохранении реально писался только фиксированный `ShiftType`, поэтому
 *    название и цвет в журнале всегда откатывались к одному из 5 стандартных.
 *
 * Теперь диалог «Смена» выбирает тип смены НАПРЯМУЮ из справочника
 * `timeTypes` (см. `ShiftEntryDialog`), а выбранный `TimeType.id` пишется в
 * `LedgerEntry.timeTypeId` — это и есть единый источник правды, общий для
 * диалога смены, журнала и справочника. Старый enum `ShiftType` оставлен
 * только для расчёта надбавки к оплате (`PayrollCalculator`, где давно
 * завязана формула множителей 1.0/1.4/1.5/2.0) — он теперь не выбирается
 * пользователем напрямую, а автоматически подбирается по названию выбранного
 * типа из справочника функцией `inferShiftTypeFromTimeType`.
 */
fun inferShiftTypeFromTimeType(timeType: TimeType?): ShiftType {
    val name = timeType?.name?.lowercase() ?: return ShiftType.DAY
    return when {
        "сверхуроч" in name -> ShiftType.OVERTIME
        "ноч" in name -> ShiftType.NIGHT
        "выходн" in name || "празд" in name -> ShiftType.HOLIDAY
        "командиров" in name -> ShiftType.OVERTIME
        else -> ShiftType.DAY
    }
}

/**
 * Фолбэк только для СТАРЫХ записей, у которых ещё нет `timeTypeId` (созданы до
 * этого исправления) — чтобы у них тоже был хоть какой-то цвет/название из
 * справочника, пока их не пересохранят через диалог «Смена».
 */
fun shiftTypeTimeTypeId(shiftType: ShiftType): String = when (shiftType) {
    ShiftType.DAY -> "tt_day"
    ShiftType.NIGHT -> "tt_night"
    ShiftType.HOLIDAY -> "tt_holiday"
    ShiftType.OVERTIME -> "tt_business_trip"
    ShiftType.WEEKEND -> "tt_unpaid_vacation"
}

/**
 * ИСПРАВЛЕНО (ТЗ: «почему когда я добавляю сверхурочную смену она всё равно
 * отображается как дневная»): «сверхурочная» — это НЕ отдельный пункт
 * справочника (в списке типов времени по умолчанию такого нет вообще), это
 * отдельная галочка «Оплата сверхурочных часов» (`overtimeEnabled`) в диалоге
 * «Смена». Раньше она влияла ТОЛЬКО на множитель в PayrollCalculator и нигде
 * не отображалась — смена с типом «Дневная смена» + включённой галочкой
 * сверхурочных везде так и продолжала называться и выглядеть как обычная
 * «Дневная смена». Теперь везде, где показывается название типа смены,
 * добавляется явная пометка.
 */
fun displayShiftLabel(baseName: String, overtimeEnabled: Boolean): String =
    if (overtimeEnabled) "$baseName · Сверхурочно" else baseName

/**
 * ИСПРАВЛЕНО (ТЗ: «в полях про деньги можно ввести только цифры»): общий
 * фильтр ввода для ВСЕХ денежных/числовых полей проекта — один и тот же код
 * вместо нескольких разных копий (`moneyInputFilter` в ShiftEntryDialog.kt,
 * FullScreenEntryDialogs.kt, `moneyInputFilterLegacy` в EntryDialogs.kt),
 * которые могли незаметно разойтись между собой.
 */
fun moneyInputFilter(input: String): String =
    input.filter { c -> c.isDigit() || c == '.' || c == ',' }

/**
 * То же самое, но для полей, где допустим ведущий минус (например, «Остаток
 * на начало периода» может быть отрицательным).
 */
fun moneySignedInputFilter(input: String): String {
    val negative = input.startsWith("-")
    val digitsOnly = input.filter { c -> c.isDigit() || c == '.' || c == ',' }
    return if (negative) "-$digitsOnly" else digitsOnly
}

/**
 * ДОБАВЛЕНО (ТЗ: «сохранения стали накладываться друг на друга»): настоящая
 * проверка пересечения по времени двух смен в пределах одного дня (с учётом
 * ночных смен, переходящих через полночь — тот же способ сравнения, что и в
 * `LedgerEntry.calculateHours()`). Раньше ничего не предупреждало пользователя,
 * что новая смена по времени накладывается на уже существующую смену того же
 * сотрудника в тот же день — теперь диалог «Смена» показывает предупреждение,
 * если это произошло, вместо того чтобы дать записям молча наложиться друг на
 * друга без единого сигнала пользователю.
 */
fun timeRangesOverlap(
    aStart: LocalTime?,
    aEnd: LocalTime?,
    bStart: LocalTime?,
    bEnd: LocalTime?
): Boolean {
    if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false
    val aStartSec = aStart.toSecondOfDay()
    val aEndSecRaw = aEnd.toSecondOfDay()
    val aEndSec = if (aEndSecRaw > aStartSec) aEndSecRaw else aEndSecRaw + 86400
    val bStartSec = bStart.toSecondOfDay()
    val bEndSecRaw = bEnd.toSecondOfDay()
    val bEndSec = if (bEndSecRaw > bStartSec) bEndSecRaw else bEndSecRaw + 86400
    return aStartSec < bEndSec && bStartSec < aEndSec
}

/**
 * ДОБАВЛЕНО (ТЗ: «чтобы человек мог добавить в смены и остальные вкладки
 * промежуток дат, а сейчас можно выбрать только одно число»): общий способ
 * получить список дат периода [start; end] включительно, используемый всеми
 * диалогами добавления записи для создания одной записи на каждый день
 * выбранного диапазона.
 */
fun dateRangeDays(start: LocalDate, end: LocalDate): List<LocalDate> {
    if (end.isBefore(start)) return listOf(start)
    return generateSequence(start) { d -> if (d.isBefore(end)) d.plusDays(1) else null }.toList()
}

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val shiftType: ShiftType = ShiftType.DAY,
    val type: EntryType = EntryType.SHIFT,
    val employeeId: String? = null,
    val organizationId: String? = null,
    val amount: Double = 0.0,
    val hours: Double = 0.0,
    val note: String = "",
    val expenseCategoryId: String? = null,
    val unitId: String? = null,
    val quantity: Double = 0.0,
    // ДОБАВЛЕНО (ТЗ «настроить правильную математику»): доплаты/удержания,
    // применённые к этой смене (переносятся из шаблона смены при применении).
    val surchargeIds: List<String> = emptyList(),
    // ДОБАВЛЕНО (диалог «Смена» по макету): неоплачиваемые перерывы (минуты),
    // признак «Оплата сверхурочных часов» и привязанный проект.
    val unpaidBreakMinutes: Int = 0,
    val overtimeEnabled: Boolean = false,
    val projectName: String = "",
    // ДОБАВЛЕНО (ТЗ: полноэкранные диалоги «Доплата, удержание» и «Запись табеля»):
    // выбранный тип доплаты/удержания (включая собственные варианты пользователя)
    // и привязка к записи справочника «Типы времени» для записей табеля.
    val adjustmentTypeName: String = "",
    val timeTypeId: String? = null,
    // ДОБАВЛЕНО (ТЗ «сделать открытие галереи/файлов настоящими»): реальные вложения
    // записи — content:// URI файлов/фото, выбранных через системный выбор файлов.
    val attachments: List<String> = emptyList()
) {
    /** Полная длительность смены по времени начала/конца (без вычета перерывов). */
    fun calculateHours(): Double {
        if (startTime == null || endTime == null) return hours
        val start = startTime.toSecondOfDay()
        val end = endTime.toSecondOfDay()
        val diffSeconds = if (end >= start) end - start else (end + 86400) - start
        return diffSeconds / 3600.0
    }

    /** Оплачиваемые часы = длительность смены минус неоплачиваемые перерывы. */
    fun calculatePaidHours(): Double {
        val raw = calculateHours() - unpaidBreakMinutes / 60.0
        return if (raw < 0.0) 0.0 else raw
    }
}

// ========== МОДЕЛИ ДЛЯ СПРАВОЧНИКОВ ==========

// 1. Типы времени
/**
 * ДОБАВЛЕНО (ТЗ: «пусть новые пользовательские типы в справочнике отображались
 * визуально и тоже на что-то влияли»): раньше у типа времени не было
 * собственного множителя оплаты — расчёт зарплаты (`PayrollCalculator`)
 * определял множитель, УГАДЫВАЯ его по русским словам в названии типа
 * (`inferShiftTypeFromTimeType`: «сверхуроч», «ноч», «выходн»/«празд»,
 * «командиров»). Из-за этого любой СВОЙ тип, придуманный пользователем в
 * справочнике («Смена А», «Дежурство» и т.п.), никогда не подбирал нужный
 * множитель — деньги считались так, будто это всегда обычная дневная смена,
 * что бы пользователь ни ввёл в название. Теперь множитель — обычное поле
 * самого типа времени (по умолчанию ×1.0), пользователь редактирует его прямо
 * в справочнике, и `PayrollCalculator` берёт именно это значение напрямую —
 * никакого угадывания по словам для новых/пользовательских типов. Угадывание
 * по названию оставлено только как запасной вариант для СТАРЫХ записей,
 * созданных до этого исправления и не имеющих `timeTypeId` вовсе.
 */
data class TimeType(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val code: String = "",
    val color: String = "#45B7D1",
    val isBuiltIn: Boolean = false,
    val payMultiplier: Double = 1.0
)

// 2. Категории расходов
data class ExpenseCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val isBuiltIn: Boolean = false
)

// 3. Единицы измерения (ТОЛЬКО ПРЕДУСТАНОВЛЕННЫЕ)
data class UnitOfMeasure(
    val id: String,
    val name: String,
    val shortName: String,
    val isBuiltIn: Boolean = true
)

// 4. Налоги
data class Tax(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val rate: Double = 0.0,
    val isBuiltIn: Boolean = false
)

// ========== ФАБРИКИ ПРЕДУСТАНОВЛЕННЫХ ДАННЫХ ==========

fun defaultUnits(): List<UnitOfMeasure> = listOf(
    UnitOfMeasure("unit_km", "Километр", "км"),
    UnitOfMeasure("unit_hour", "Час", "ч"),
    UnitOfMeasure("unit_piece", "Штука", "шт")
)

// ДОБАВЛЕНО (ТЗ: «Сделать в справочнике встроенными данные категории (с
// возможностью редактирования)»): раньше все три фабрики возвращали пустой
// список — экраны «Типы времени» / «Категории расходов» / «Налоги» были
// пустыми при первом запуске. Данные ниже и порядок цветов взяты из
// референс-скриншотов в ТЗ. isBuiltIn = true просто помечает происхождение
// записи (сама фабрика), удаление/редактирование этим не ограничено —
// экраны и так уже поддерживают и то, и другое.
fun defaultTimeTypes(): List<TimeType> = listOf(
    TimeType(id = "tt_sick", name = "Больничный", code = "Б", color = "#8395A7", isBuiltIn = true, payMultiplier = 1.0),
    TimeType(id = "tt_evening", name = "Вечерняя смена", code = "В", color = "#48DBFB", isBuiltIn = true, payMultiplier = 1.0),
    TimeType(id = "tt_holiday", name = "Выходные и нерабочие праздничные", code = "ВП", color = "#FF6FB7", isBuiltIn = true, payMultiplier = 2.0),
    TimeType(id = "tt_day", name = "Дневная смена", code = "Д", color = "#5CA02F", isBuiltIn = true, payMultiplier = 1.0),
    TimeType(id = "tt_unpaid_vacation", name = "Неоплачиваемый отпуск", code = "ДО", color = "#10AC84", isBuiltIn = true, payMultiplier = 1.0),
    TimeType(id = "tt_night", name = "Ночная смена", code = "Н", color = "#8395A7", isBuiltIn = true, payMultiplier = 1.4),
    TimeType(id = "tt_paid_vacation", name = "Оплачиваемый отпуск", code = "ОТ", color = "#5F27CD", isBuiltIn = true, payMultiplier = 1.0),
    TimeType(id = "tt_business_trip", name = "Служебная командировка", code = "К", color = "#FF9F43", isBuiltIn = true, payMultiplier = 1.5)
)

fun defaultExpenseCategories(): List<ExpenseCategory> = listOf(
    ExpenseCategory(id = "ec_transport", name = "Проезд", isBuiltIn = true),
    ExpenseCategory(id = "ec_accommodation", name = "Проживание", isBuiltIn = true),
    ExpenseCategory(id = "ec_other", name = "Прочее", isBuiltIn = true),
    ExpenseCategory(id = "ec_fuel", name = "Топливо/километраж", isBuiltIn = true)
)

fun defaultTaxes(): List<Tax> = listOf(
    Tax(id = "tax_ndfl", name = "НДФЛ", rate = 13.0, isBuiltIn = true)
)

data class AppState(
    val employees: List<Employee> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val entries: List<LedgerEntry> = emptyList(),
    val openingBalance: Double = 0.0,
    val surcharges: List<Surcharge> = emptyList(),
    val shiftTemplates: List<ShiftTemplate> = emptyList(),
    val timeTypes: List<TimeType> = defaultTimeTypes(),
    val expenseCategories: List<ExpenseCategory> = defaultExpenseCategories(),
    val units: List<UnitOfMeasure> = defaultUnits(),
    val taxes: List<Tax> = defaultTaxes()
)

// ========== ДОПЛАТЫ И ШАБЛОНЫ ==========

enum class SurchargeKind { BONUS, DEDUCTION }
enum class SurchargeCalcType { FIXED_PER_SHIFT, PER_HOUR, PERCENT }

data class Surcharge(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val kind: SurchargeKind = SurchargeKind.BONUS,
    val calcType: SurchargeCalcType = SurchargeCalcType.FIXED_PER_SHIFT,
    val amount: Double = 0.0,
    val taxable: Boolean = true,
    val activeWeekdays: Set<Int> = emptySet(),
    val isBuiltIn: Boolean = false
)

data class ShiftTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val employeeId: String? = null,
    val organizationId: String? = null,
    val shiftTypeName: String = "",
    val projectName: String = "",
    val surchargeIds: List<String> = emptyList(),
    val comment: String = ""
)

fun defaultSurchargeLibrary(): List<Surcharge> = listOf(
    Surcharge(
        id = "builtin_premium", name = "Премия",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.FIXED_PER_SHIFT, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_weekend", name = "Выходные дни",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PERCENT, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_night", name = "Ночные часы",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PER_HOUR, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_holiday", name = "Праздничные дни",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PERCENT, isBuiltIn = true
    ),
    Surcharge(
        id = "builtin_piecework", name = "Пример сдельной работы",
        kind = SurchargeKind.BONUS, calcType = SurchargeCalcType.PER_HOUR, isBuiltIn = true
    )
)