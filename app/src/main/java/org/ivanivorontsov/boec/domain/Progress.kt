package org.ivanivorontsov.boec.domain

import org.ivanivorontsov.boec.content.Exercises
import org.ivanivorontsov.boec.model.LogEntry
import org.ivanivorontsov.boec.model.PlannedExercise

object Dates {
    /** ISO day of week (1 = Monday) for an epoch day. 1970-01-01 was a Thursday. */
    fun isoDay(epochDay: Long): Int = (Math.floorMod(epochDay + 3, 7L) + 1).toInt()
    fun mondayOf(epochDay: Long): Long = epochDay - (isoDay(epochDay) - 1)
}

object Streaks {
    /** Consecutive planned training days completed, going back from today (today not done yet does not break it). */
    fun planStreak(logDays: Set<Long>, plannedIsoDays: Set<Int>, today: Long): Int {
        if (plannedIsoDays.isEmpty()) return 0
        var count = 0
        var d = today
        repeat(730) {
            if (Dates.isoDay(d) in plannedIsoDays) {
                if (d in logDays) count++
                else if (d != today) return count
            } else if (d in logDays) count++ // bonus sessions on rest days also count
            d--
        }
        return count
    }

    /** Consecutive weeks (Mon–Sun) with at least [perWeek] sessions; the current week counts only once met. */
    fun weekStreak(logDays: Collection<Long>, perWeek: Int, today: Long): Int {
        if (perWeek <= 0) return 0
        val byWeek = logDays.groupingBy { Dates.mondayOf(it) }.eachCount()
        var monday = Dates.mondayOf(today)
        var count = 0
        if ((byWeek[monday] ?: 0) >= perWeek) count++
        monday -= 7
        while ((byWeek[monday] ?: 0) >= perWeek) { count++; monday -= 7 }
        return count
    }

    fun minutesPerWeek(log: List<LogEntry>, today: Long, weeks: Int): List<Int> {
        val start = Dates.mondayOf(today) - 7L * (weeks - 1)
        val arr = IntArray(weeks)
        for (e in log) {
            val w = ((Dates.mondayOf(e.epochDay) - start) / 7).toInt()
            if (w in 0 until weeks) arr[w] += e.durationSec / 60
        }
        return arr.toList()
    }
}

object Progression {
    /** Double progression advice from the reps achieved in each set and the session RPE. */
    fun advice(pe: PlannedExercise, repsDone: List<Int>, rpe: Int?): String {
        val ex = Exercises.find(pe.exerciseId) ?: return ""
        if (repsDone.isEmpty()) return "Начни с нижней границы диапазона."
        val unit = if (pe.timed) "с" else "повт."
        return when {
            repsDone.all { it >= pe.repsMax } && (rpe ?: 8) <= 8 -> {
                val harder = ex.harder?.let { Exercises.find(it)?.name }
                if (harder != null) "Отлично! В следующий раз — «$harder» (начни с ${pe.repsMin} $unit)."
                else "Отлично! Добавь 2–5 % веса или замедли опускание до 3–4 с."
            }
            repsDone.any { it < pe.repsMin } || (rpe ?: 0) >= 10 -> {
                val easier = ex.easier?.let { Exercises.find(it)?.name }
                if (easier != null) "Тяжело — можно вернуться к «$easier» и закрепить технику." else "Тяжело — оставь нагрузку и добивайся чистой техники."
            }
            else -> "Добавляй по 1 ${if (pe.timed) "–5 с" else "повтору"} за тренировку до ${pe.repsMax} $unit."
        }
    }
}
