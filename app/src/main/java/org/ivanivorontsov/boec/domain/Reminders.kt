package org.ivanivorontsov.boec.domain

import org.ivanivorontsov.boec.model.AlertMode
import org.ivanivorontsov.boec.model.Profile
import org.ivanivorontsov.boec.model.Reminder
import org.ivanivorontsov.boec.model.ReminderKind
import org.ivanivorontsov.boec.model.SessionType
import org.ivanivorontsov.boec.model.Snooze
import org.ivanivorontsov.boec.model.WeekPlan
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Pure time math for reminders (no Android dependencies, unit-tested). */
object ReminderMath {
    fun maskOf(isoDays: Collection<Int>): Int = isoDays.filter { it in 1..7 }.fold(0) { acc, d -> acc or (1 shl (d - 1)) }
    fun daysOf(mask: Int): List<Int> = (1..7).filter { mask and (1 shl (it - 1)) != 0 }
    fun monthsMaskOf(months: Iterable<Int>): Int = months.filter { it in 1..12 }.fold(0) { acc, m -> acc or (1 shl (m - 1)) }

    fun matchesDate(r: Reminder, date: LocalDate): Boolean {
        if (r.monthsMask and (1 shl (date.monthValue - 1)) == 0) return false
        return if (r.dayOfMonth > 0) date.dayOfMonth == minOf(r.dayOfMonth, date.lengthOfMonth())
        else r.daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
    }

    fun timesOfDay(r: Reminder): List<LocalTime> {
        val start = LocalTime.of(r.hour.coerceIn(0, 23), r.minute.coerceIn(0, 59))
        if (r.repeatEveryMin <= 0) return listOf(start)
        val startMin = r.hour * 60 + r.minute
        val endMin = r.untilHour * 60 + r.untilMinute
        if (endMin <= startMin) return listOf(start)
        return (startMin..endMin step r.repeatEveryMin.coerceAtLeast(5)).map { LocalTime.of(it / 60, it % 60) }
    }

    /** Next trigger strictly after [now], or null if the reminder can never fire. */
    fun next(r: Reminder, now: ZonedDateTime): ZonedDateTime? {
        if (r.daysMask == 0 && r.dayOfMonth <= 0) return null
        if (r.monthsMask and 0xFFF == 0) return null
        val zone = now.zone
        val times = timesOfDay(r)
        var date = now.toLocalDate()
        repeat(400) {
            if (matchesDate(r, date)) {
                for (t in times) {
                    val cand = ZonedDateTime.of(date, t, zone)
                    if (cand.toInstant().isAfter(now.toInstant())) return cand
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    fun describe(r: Reminder): String {
        val time = "%02d:%02d".format(r.hour, r.minute)
        val days = when {
            r.dayOfMonth > 0 -> "${r.dayOfMonth}-го числа"
            r.daysMask == 0b1111111 -> "каждый день"
            r.daysMask == 0b0011111 -> "по будням"
            r.daysMask == 0b1100000 -> "по выходным"
            else -> daysOf(r.daysMask).joinToString(", ") { DAY_SHORT[it - 1] }
        }
        val rep = if (r.repeatEveryMin > 0) " · каждые ${r.repeatEveryMin} мин до %02d:%02d".format(r.untilHour, r.untilMinute) else ""
        val season = if (r.monthsMask and 0xFFF != 0xFFF) " · " + seasonText(r.monthsMask) else ""
        return "$days, $time$rep$season"
    }

    private fun seasonText(mask: Int): String {
        val ms = (1..12).filter { mask and (1 shl (it - 1)) != 0 }
        if (ms.isEmpty()) return "никогда"
        return "${MONTH_SHORT[ms.first() - 1]}–${MONTH_SHORT[ms.last() - 1]}"
    }

    val DAY_SHORT = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val DAY_FULL = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
    val MONTH_SHORT = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
}

/** One alarm the scheduler must register with AlarmManager. */
data class ScheduledAlarm(val requestCode: Int, val reminderId: String, val atMillis: Long, val snooze: Boolean, val mode: AlertMode)

object AlarmPlan {
    fun requestCode(reminderId: String, snooze: Boolean): Int {
        val h = reminderId.hashCode() and 0x3FFFFFFF
        return if (snooze) h or 0x40000000 else h
    }

    fun compute(reminders: List<Reminder>, snoozes: List<Snooze>, nowMillis: Long, zone: ZoneId): List<ScheduledAlarm> {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val byId = reminders.associateBy { it.id }
        val regular = reminders.filter { it.enabled }.mapNotNull { r ->
            ReminderMath.next(r, now)?.let { ScheduledAlarm(requestCode(r.id, false), r.id, it.toInstant().toEpochMilli(), false, r.mode) }
        }
        val snoozed = snoozes.filter { it.atMillis > nowMillis }.mapNotNull { s ->
            val r = byId[s.reminderId] ?: return@mapNotNull null
            ScheduledAlarm(requestCode(r.id, true), r.id, s.atMillis, true, r.mode)
        }.groupBy { it.reminderId }.map { (_, v) -> v.minBy { it.atMillis } }
        return (regular + snoozed).sortedBy { it.atMillis }
    }

    fun snoozeUntil(nowMillis: Long, minutes: Int) = nowMillis + minutes.coerceIn(1, 120) * 60_000L
}

object ReminderFactory {
    fun workoutId(isoDay: Int) = "workout_$isoDay"

    /** Workout reminders (call mode by default) for each training day; keeps user's enabled/mode choices. */
    fun forPlan(profile: Profile, plan: WeekPlan, existing: List<Reminder>): List<Reminder> {
        val old = existing.associateBy { it.id }
        return plan.days.map { d ->
            val id = workoutId(d.isoDay)
            val prev = old[id]
            val kind = if (d.session.type == SessionType.SKILLS) ReminderKind.SKILL else ReminderKind.WORKOUT
            Reminder(
                id = id, kind = kind,
                title = d.session.title,
                text = "≈${d.session.estimatedMin} мин · ${d.session.exercises.size + d.session.drills.size} упражнений. Пора тренироваться!",
                enabled = prev?.enabled ?: true,
                mode = prev?.mode ?: AlertMode.CALL,
                daysMask = 1 shl (d.isoDay - 1),
                hour = profile.hour, minute = profile.minute,
                planDay = d.isoDay,
            )
        }
    }

    fun defaults(): List<Reminder> = listOf(
        Reminder("water", ReminderKind.WATER, "Выпей воды", "Стакан воды. Ориентир для взрослых — около 2–2,5 л жидкости в день с учётом еды (EFSA); в жару и при нагрузке больше.",
            enabled = true, hour = 9, minute = 0, repeatEveryMin = 120, untilHour = 21, untilMinute = 0),
        Reminder("posture", ReminderKind.POSTURE, "Встань и разомнись", "Посидел час — встань на 2–3 минуты: плечи назад, 10 приседаний, пройдись. ВОЗ советует сокращать время сидя.",
            enabled = false, daysMask = 0b0011111, hour = 10, minute = 0, repeatEveryMin = 60, untilHour = 18, untilMinute = 0),
        Reminder("eyes", ReminderKind.EYES, "Глаза: 20-20-20", "Каждые 20 минут у экрана смотри 20 секунд на предмет в 6 метрах (AAO).",
            enabled = false, daysMask = 0b0011111, hour = 10, minute = 20, repeatEveryMin = 60, untilHour = 18, untilMinute = 20),
        Reminder("sleep", ReminderKind.SLEEP, "Готовься ко сну", "Через 30 минут — в кровать. Взрослым нужно 7+ часов сна (AASM). Убери экран, проветри комнату.",
            enabled = true, hour = 22, minute = 30),
        Reminder("stretch", ReminderKind.STRETCH, "Растяжка и мобильность", "10 минут: грудной отдел, тазобедренные, плечи, задняя поверхность бедра. Плавно, без боли.",
            enabled = false, daysMask = ReminderMath.maskOf(listOf(2, 4, 6)), hour = 20, minute = 30),
        Reminder("ticks", ReminderKind.TICKS, "Осмотр на клещей", "Был на природе? Осмотри подмышки, пах, за ушами, волосы, под коленями. Проверь питомца.",
            enabled = true, hour = 20, minute = 0, monthsMask = ReminderMath.monthsMaskOf(4..10)),
        Reminder("firstaid", ReminderKind.FIRST_AID, "Проверь аптечку", "Бинты, антисептик, жгут, антигистаминное, обезболивающее, пинцет, перчатки, сроки годности. Дома и в машине.",
            enabled = true, dayOfMonth = 1, hour = 12, minute = 0),
    )
}
