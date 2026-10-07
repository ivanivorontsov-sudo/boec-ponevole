package org.ivanivorontsov.boec

import org.ivanivorontsov.boec.domain.AlarmPlan
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.ReminderFactory
import org.ivanivorontsov.boec.domain.ReminderMath
import org.ivanivorontsov.boec.domain.ScheduleGenerator
import org.ivanivorontsov.boec.domain.Streaks
import org.ivanivorontsov.boec.model.AlertMode
import org.ivanivorontsov.boec.model.Profile
import org.ivanivorontsov.boec.model.Reminder
import org.ivanivorontsov.boec.model.ReminderKind
import org.ivanivorontsov.boec.model.Snooze
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmSchedulingTest {
    private val vlad = ZoneId.of("Asia/Vladivostok")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int, zone: ZoneId = vlad) = ZonedDateTime.of(LocalDateTime.of(y, m, d, h, min), zone)
    private fun r(id: String = "r", days: Int = 0b1111111, h: Int = 19, m: Int = 0, every: Int = 0, uh: Int = 0, um: Int = 0, months: Int = 0xFFF, dom: Int = 0, mode: AlertMode = AlertMode.NOTIFICATION) =
        Reminder(id, ReminderKind.CUSTOM, "t", "x", daysMask = days, hour = h, minute = m, repeatEveryMin = every, untilHour = uh, untilMinute = um, monthsMask = months, dayOfMonth = dom, mode = mode)

    @Test fun nextIsLaterToday() {
        // 2026-10-07 is a Wednesday
        assertEquals(at(2026, 10, 7, 19, 0), ReminderMath.next(r(), at(2026, 10, 7, 10, 0)))
    }

    @Test fun nextIsStrictlyAfterNow() {
        assertEquals(at(2026, 10, 8, 19, 0), ReminderMath.next(r(), at(2026, 10, 7, 19, 0)))
    }

    @Test fun respectsWeekdays() {
        val mondayFriday = ReminderMath.maskOf(listOf(1, 5))
        // Wednesday 20:00 -> Friday 19:00
        assertEquals(at(2026, 10, 9, 19, 0), ReminderMath.next(r(days = mondayFriday), at(2026, 10, 7, 20, 0)))
        // Friday 20:00 -> Monday 19:00
        assertEquals(at(2026, 10, 12, 19, 0), ReminderMath.next(r(days = mondayFriday), at(2026, 10, 9, 20, 0)))
    }

    @Test fun intervalWithinWindow() {
        val water = r(h = 9, m = 0, every = 120, uh = 21, um = 0)
        assertEquals(at(2026, 10, 7, 11, 0), ReminderMath.next(water, at(2026, 10, 7, 10, 30)))
        assertEquals(at(2026, 10, 7, 21, 0), ReminderMath.next(water, at(2026, 10, 7, 20, 0)))
        assertEquals(at(2026, 10, 8, 9, 0), ReminderMath.next(water, at(2026, 10, 7, 21, 0)))
        assertEquals(7, ReminderMath.timesOfDay(water).size)
    }

    @Test fun seasonalMonthsSkipWinter() {
        val ticks = r(h = 20, months = ReminderMath.monthsMaskOf(4..10))
        assertEquals(at(2026, 10, 31, 20, 0), ReminderMath.next(ticks, at(2026, 10, 31, 12, 0)))
        assertEquals(at(2027, 4, 1, 20, 0), ReminderMath.next(ticks, at(2026, 10, 31, 21, 0)))
    }

    @Test fun monthlyClampsToShortMonths() {
        val monthly = r(h = 12, dom = 31)
        assertEquals(at(2026, 11, 30, 12, 0), ReminderMath.next(monthly, at(2026, 11, 1, 0, 0)))
        assertEquals(at(2027, 2, 28, 12, 0), ReminderMath.next(monthly, at(2027, 2, 1, 0, 0)))
    }

    @Test fun impossibleReminderReturnsNull() {
        assertNull(ReminderMath.next(r(days = 0), at(2026, 10, 7, 0, 0)))
        assertNull(ReminderMath.next(r(months = 0), at(2026, 10, 7, 0, 0)))
    }

    @Test fun dstGapIsHandled() {
        // Europe/Berlin: 2027-03-28 02:00 -> 03:00. A 02:30 reminder must still fire that day (shifted), never skipped.
        val berlin = ZoneId.of("Europe/Berlin")
        val next = ReminderMath.next(r(h = 2, m = 30), at(2027, 3, 28, 0, 0, berlin))!!
        assertEquals(LocalDate.of(2027, 3, 28), next.toLocalDate())
        assertTrue(next.isAfter(at(2027, 3, 28, 0, 0, berlin)))
    }

    @Test fun planProducesComputableAlarmsAndUniqueRequestCodes() {
        val profile = Profile(days = listOf(1, 3, 5), hour = 7, minute = 30)
        val plan = ScheduleGenerator.generate(profile)
        val reminders = ReminderFactory.defaults() + ReminderFactory.forPlan(profile, plan, emptyList())
        val now = at(2026, 10, 7, 8, 0).toInstant().toEpochMilli()
        val alarms = AlarmPlan.compute(reminders, emptyList(), now, vlad)
        assertEquals(reminders.count { it.enabled }, alarms.size)
        assertTrue(alarms.all { it.atMillis > now })
        assertEquals(alarms.size, alarms.map { it.requestCode }.toSet().size)
        // Workout reminders default to call mode on their own weekday.
        val wed = alarms.first { it.reminderId == ReminderFactory.workoutId(3) }
        assertEquals(AlertMode.CALL, wed.mode)
        assertEquals(at(2026, 10, 14, 7, 30).toInstant().toEpochMilli(), wed.atMillis) // today 07:30 passed -> next Wednesday
        // sorted ascending
        assertEquals(alarms.sortedBy { it.atMillis }, alarms)
    }

    @Test fun snoozesAreScheduledSeparatelyAndStaleOnesIgnored() {
        val rem = r(id = "w", mode = AlertMode.CALL)
        val now = at(2026, 10, 7, 19, 0).toInstant().toEpochMilli()
        val snoozeAt = AlarmPlan.snoozeUntil(now, 10)
        assertEquals(now + 600_000, snoozeAt)
        val alarms = AlarmPlan.compute(listOf(rem), listOf(Snooze("w", snoozeAt), Snooze("w", now - 1000), Snooze("unknown", snoozeAt)), now, vlad)
        assertEquals(2, alarms.size)
        val s = alarms.first { it.snooze }
        assertEquals(snoozeAt, s.atMillis)
        assertTrue(s.requestCode != alarms.first { !it.snooze }.requestCode)
        assertEquals(AlertMode.CALL, s.mode)
    }

    @Test fun snoozeOfDisabledReminderStillFires() {
        val rem = r(id = "test").copy(enabled = false)
        val now = 1_000_000L
        val alarms = AlarmPlan.compute(listOf(rem), listOf(Snooze("test", now + 10_000)), now, vlad)
        assertEquals(listOf(now + 10_000), alarms.map { it.atMillis })
    }

    @Test fun forPlanKeepsUserChoices() {
        val profile = Profile(days = listOf(2, 4))
        val plan = ScheduleGenerator.generate(profile)
        val first = ReminderFactory.forPlan(profile, plan, emptyList())
        val edited = first.map { if (it.planDay == 2) it.copy(enabled = false, mode = AlertMode.NOTIFICATION) else it }
        val again = ReminderFactory.forPlan(profile.copy(hour = 8), plan, edited)
        val tue = again.first { it.planDay == 2 }
        assertEquals(false, tue.enabled); assertEquals(AlertMode.NOTIFICATION, tue.mode); assertEquals(8, tue.hour)
    }

    @Test fun streaks() {
        val wed = LocalDate.of(2026, 10, 7).toEpochDay()
        assertEquals(3, Dates.isoDay(wed))
        val mon = wed - 2
        val prevFri = wed - 5; val prevWed = wed - 7; val prevMon = wed - 9
        val planned = setOf(1, 3, 5)
        // Today (Wed) not done yet: still counts Mon + previous week.
        assertEquals(4, Streaks.planStreak(setOf(mon, prevFri, prevWed, prevMon), planned, wed))
        // Missed previous Friday breaks it.
        assertEquals(1, Streaks.planStreak(setOf(mon, prevWed, prevMon), planned, wed))
        assertEquals(1, Streaks.weekStreak(listOf(prevFri, prevWed, prevMon), 3, wed))
        assertEquals(0, Streaks.weekStreak(listOf(prevFri, prevWed), 3, wed))
    }
}
