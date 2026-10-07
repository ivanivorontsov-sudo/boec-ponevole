package org.ivanivorontsov.boec.domain

import org.ivanivorontsov.boec.content.Exercises
import org.ivanivorontsov.boec.content.Skills
import org.ivanivorontsov.boec.model.DayPlan
import org.ivanivorontsov.boec.model.Equipment
import org.ivanivorontsov.boec.model.Exercise
import org.ivanivorontsov.boec.model.Goal
import org.ivanivorontsov.boec.model.Level
import org.ivanivorontsov.boec.model.Muscle
import org.ivanivorontsov.boec.model.PlannedDrill
import org.ivanivorontsov.boec.model.PlannedExercise
import org.ivanivorontsov.boec.model.Profile
import org.ivanivorontsov.boec.model.Session
import org.ivanivorontsov.boec.model.SessionType
import org.ivanivorontsov.boec.model.SessionType.*
import org.ivanivorontsov.boec.model.Slot
import org.ivanivorontsov.boec.model.WeekPlan
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Builds a weekly plan from the user's days, minutes, goal, level and equipment.
 *
 * Evidence used (see Science.sources): every major muscle ≥ 2×/week (Schoenfeld 2016, ACSM 2026),
 * ≥ 2 sets per exercise and ~10+ weekly sets per muscle for hypertrophy (Schoenfeld 2017, ACSM 2026),
 * longer rests for compound/strength work (Grgic 2018), RPE-based effort (Zourdos 2016),
 * a deload every 5th week, WHO 2020 aerobic minutes on extra days.
 */
object ScheduleGenerator {

    const val DELOAD_EVERY = 5
    private val compound = setOf(Slot.SQUAT, Slot.HINGE, Slot.LUNGE, Slot.HPUSH, Slot.VPUSH, Slot.HPULL, Slot.VPULL)
    private val cautiousExcluded = setOf("vp_pike", "vp_pike_elev", "cd_burpee", "cd_sprint", "hi_nordic", "hp_archer")

    fun isDeload(weekIndex: Int) = weekIndex % DELOAD_EVERY == DELOAD_EVERY - 1

    fun dayTypes(goal: Goal, level: Level, n: Int): List<SessionType> {
        val c = n.coerceIn(1, 7)
        val base: List<SessionType> = when (goal) {
            Goal.DEFENSE -> listOf(FULL_A, SKILLS, FULL_B, SKILLS, FULL_C, CARDIO, CARDIO).let {
                when (c) { 1 -> listOf(FULL_A); 2 -> listOf(FULL_A, FULL_B); else -> it.take(c) }
            }
            Goal.MUSCLE -> when (c) {
                1 -> listOf(FULL_A); 2 -> listOf(FULL_A, FULL_B); 3 -> listOf(FULL_A, FULL_B, FULL_C)
                4 -> listOf(UPPER, LOWER, UPPER, LOWER)
                5 -> if (level == Level.BEGINNER) listOf(UPPER, LOWER, CARDIO, UPPER, LOWER) else listOf(UPPER, LOWER, PUSH, PULL, LEGS)
                6 -> if (level == Level.BEGINNER) listOf(UPPER, LOWER, CARDIO, UPPER, LOWER, CARDIO) else listOf(PUSH, PULL, LEGS, PUSH, PULL, LEGS)
                else -> if (level == Level.BEGINNER) listOf(UPPER, LOWER, CARDIO, UPPER, LOWER, CARDIO, CARDIO) else listOf(PUSH, PULL, LEGS, PUSH, PULL, LEGS, CARDIO)
            }
            Goal.HEALTH, Goal.STRENGTH -> when (c) {
                1 -> listOf(FULL_A); 2 -> listOf(FULL_A, FULL_B); 3 -> listOf(FULL_A, FULL_B, FULL_C)
                4 -> if (level == Level.BEGINNER) listOf(FULL_A, CARDIO, FULL_B, FULL_C) else listOf(UPPER, LOWER, UPPER, LOWER)
                5 -> listOf(UPPER, LOWER, CARDIO, UPPER, LOWER)
                6 -> listOf(UPPER, LOWER, CARDIO, UPPER, LOWER, CARDIO)
                else -> listOf(UPPER, LOWER, CARDIO, UPPER, LOWER, CARDIO, CARDIO)
            }
        }
        return base
    }

    private data class Template(val main: List<Slot>, val extra: List<Slot>)

    private fun template(type: SessionType, goal: Goal): Template {
        val t = when (type) {
            FULL_A -> Template(listOf(Slot.SQUAT, Slot.HPUSH, Slot.HPULL, Slot.HINGE, Slot.CORE), listOf(Slot.VPUSH, Slot.CALF, Slot.GRIP))
            FULL_B -> Template(listOf(Slot.LUNGE, Slot.HPUSH, Slot.VPULL, Slot.HINGE, Slot.CORE), listOf(Slot.VPUSH, Slot.TRICEPS, Slot.BICEPS))
            FULL_C -> Template(listOf(Slot.SQUAT, Slot.VPUSH, Slot.HPULL, Slot.LUNGE, Slot.CORE), listOf(Slot.HPUSH, Slot.CALF, Slot.GRIP))
            UPPER -> Template(listOf(Slot.HPUSH, Slot.HPULL, Slot.VPUSH, Slot.VPULL), listOf(Slot.CORE, Slot.BICEPS, Slot.TRICEPS, Slot.GRIP))
            LOWER -> Template(listOf(Slot.SQUAT, Slot.HINGE, Slot.LUNGE, Slot.CORE), listOf(Slot.CALF, Slot.HINGE))
            PUSH -> Template(listOf(Slot.HPUSH, Slot.VPUSH, Slot.HPUSH, Slot.TRICEPS), listOf(Slot.CORE))
            PULL -> Template(listOf(Slot.VPULL, Slot.HPULL, Slot.HPULL, Slot.BICEPS), listOf(Slot.GRIP, Slot.CORE))
            LEGS -> Template(listOf(Slot.SQUAT, Slot.HINGE, Slot.LUNGE, Slot.CALF), listOf(Slot.CORE))
            SKILLS, CARDIO -> Template(emptyList(), emptyList())
        }
        if (goal == Goal.MUSCLE && type == UPPER) return t.copy(extra = listOf(Slot.HPUSH) + t.extra)
        // Self-defence: grip and neck strength help with clinches and whiplash; put them first among extras.
        return if (goal == Goal.DEFENSE && t.main.isNotEmpty()) t.copy(extra = listOf(Slot.GRIP, Slot.NECK) + t.extra.filter { it != Slot.GRIP }) else t
    }

    private fun focusSlots(focus: List<Muscle>): List<Slot> = focus.mapNotNull {
        when (it) {
            Muscle.CHEST -> Slot.HPUSH; Muscle.LATS -> Slot.VPULL; Muscle.UPPER_BACK -> Slot.HPULL
            Muscle.SHOULDERS -> Slot.VPUSH; Muscle.BICEPS -> Slot.BICEPS; Muscle.TRICEPS -> Slot.TRICEPS
            Muscle.FOREARMS -> Slot.GRIP; Muscle.CORE -> Slot.CORE; Muscle.LOWER_BACK -> Slot.HINGE
            Muscle.GLUTES -> Slot.HINGE; Muscle.QUADS -> Slot.SQUAT; Muscle.HAMSTRINGS -> Slot.HINGE
            Muscle.CALVES -> Slot.CALF; Muscle.NECK -> Slot.NECK
        }
    }

    fun available(eq: Collection<Equipment>, cautious: Boolean): List<Exercise> {
        val have = eq.toSet() + Equipment.NONE
        return Exercises.all.filter { it.equipment in have && (!cautious || (it.id !in cautiousExcluded && it.difficulty <= 3)) }
    }

    fun targetDifficulty(level: Level, cautious: Boolean) = when {
        cautious -> 1
        level == Level.BEGINNER -> 2
        else -> 3
    }

    private class Picker(private val pool: List<Exercise>, private val target: Int) {
        private val usedWeek = HashMap<String, Int>()
        fun pick(slot: Slot, exclude: Set<String>): Exercise? {
            val cands = pool.filter { it.slot == slot && it.id !in exclude }
            if (cands.isEmpty()) return null
            val best = cands.withIndex().minWith(compareBy({ abs(it.value.difficulty - target) * 2 + (usedWeek[it.value.id] ?: 0) }, { it.index })).value
            usedWeek[best.id] = (usedWeek[best.id] ?: 0) + 1
            return best
        }
    }

    data class Dose(val repsMin: Int, val repsMax: Int, val rest: Int)

    fun dose(goal: Goal, level: Level, ex: Exercise): Dose {
        if (ex.slot == Slot.CONDITIONING) return Dose(20, 30, 40)
        if (ex.timed) return if (level == Level.BEGINNER) Dose(15, 30, 45) else Dose(25, 45, 60)
        val isCompound = ex.slot in compound
        return when (goal) {
            Goal.STRENGTH -> if (isCompound) Dose(5, 8, 180) else Dose(8, 12, 90)
            Goal.MUSCLE -> if (isCompound) Dose(8, 12, 120) else Dose(10, 15, 75)
            Goal.HEALTH -> if (isCompound) Dose(8, 12, 90) else Dose(10, 15, 60)
            Goal.DEFENSE -> if (isCompound) Dose(6, 10, 120) else Dose(8, 12, 60)
        }
    }

    fun setSeconds(pe: PlannedExercise): Int {
        val ex = Exercises.get(pe.exerciseId)
        val sides = if (ex.perSide) 2 else 1
        val avg = (pe.repsMin + pe.repsMax) / 2.0
        val work = if (pe.timed) avg * sides else avg * 3.0 * sides + 10
        return work.roundToInt()
    }

    fun exerciseSeconds(pe: PlannedExercise) = pe.sets * (setSeconds(pe) + pe.restSec)

    fun drillSeconds(d: PlannedDrill): Int {
        val work = if (d.reps > 0) d.reps * 4 else d.workSec
        return d.rounds * (work + d.restSec)
    }

    private fun warmupMin(minutes: Int) = if (minutes < 25) 4 else 6
    private fun cooldownMin(minutes: Int) = if (minutes < 25) 2 else 4

    /** Short self-defence block appended to strength days for the DEFENSE goal. */
    private fun defenseBlock(variant: Int, cautious: Boolean): List<PlannedDrill> = when (variant % 3) {
        0 -> listOf(PlannedDrill("st_stance", 1, 45, 15), PlannedDrill("cd_rounds", 2, if (cautious) 60 else 120, 60))
        1 -> listOf(PlannedDrill("gr_standup", 2, 0, 30, reps = 10), PlannedDrill("rl_wrist", 2, 0, 30, reps = 10), PlannedDrill("st_footwork", 1, 90, 30))
        else -> listOf(PlannedDrill("aw_voice", 1, 60, 15), PlannedDrill("st_angle", 2, 60, 30), PlannedDrill("sk_pushkick", 2, 45, 30))
    }

    private fun miniDefenseBlock(variant: Int): List<PlannedDrill> = when (variant % 3) {
        0 -> listOf(PlannedDrill("cd_rounds", 1, 120, 0))
        1 -> listOf(PlannedDrill("gr_standup", 1, 0, 15, reps = 10), PlannedDrill("rl_wrist", 1, 0, 0, reps = 10))
        else -> listOf(PlannedDrill("st_angle", 1, 60, 15), PlannedDrill("sk_pushkick", 1, 45, 0))
    }

    private fun skillsDrills(variant: Int, level: Level, cautious: Boolean): List<PlannedDrill> {
        val r = if (level == Level.BEGINNER) 2 else 3
        val a = listOf(
            PlannedDrill("st_stance", 1, 60, 15),
            PlannedDrill("st_footwork", r, 90, 45),
            PlannedDrill("sk_guard", 2, 60, 30),
            PlannedDrill("sk_jab", r, 60, 30),
            PlannedDrill("sk_cross", r, 60, 30),
            PlannedDrill("rl_wrist", 2, 0, 30, reps = 10),
            PlannedDrill("rl_clothes", 2, 60, 30),
            PlannedDrill("sk_knee", 2, 45, 30),
            if (cautious) PlannedDrill("cd_rounds", 2, 60, 60) else PlannedDrill("cd_rounds", r, 120, 60),
            PlannedDrill("aw_voice", 1, 60, 15),
        )
        val b = listOf(
            PlannedDrill("aw_scan", 1, 60, 15),
            PlannedDrill("st_angle", r, 60, 30),
            PlannedDrill("gr_backfall", 2, 0, 45, reps = 8),
            PlannedDrill("gr_sidefall", 2, 0, 45, reps = 8),
            PlannedDrill("gr_standup", r, 0, 30, reps = 10),
            PlannedDrill("gr_shrimp", 2, 60, 30),
            PlannedDrill("gr_bridge", 2, 0, 30, reps = 8),
            PlannedDrill("sk_palm", 2, 45, 30),
            PlannedDrill("sk_pushkick", 2, 45, 30),
            PlannedDrill("aw_exit", 2, 0, 45, reps = 4),
            if (cautious) PlannedDrill("cd_rounds", 2, 60, 60) else PlannedDrill("cd_sprints", if (level == Level.BEGINNER) 6 else 8, 15, 45),
        )
        return if (variant % 2 == 0) a else b
    }

    fun buildStrengthSession(
        type: SessionType, p: Profile, picker: (Slot, Set<String>) -> Exercise?, deload: Boolean, defenseVariant: Int,
    ): Session {
        val tpl = template(type, p.goal)
        val minutes = p.minutes.coerceIn(15, 120)
        val budget = minutes * 60
        val warm = warmupMin(minutes)
        val cool = cooldownMin(minutes)
        val drills = when {
            p.goal != Goal.DEFENSE || minutes < 30 -> emptyList()
            minutes < 40 -> miniDefenseBlock(defenseVariant)
            else -> defenseBlock(defenseVariant, p.cautious)
        }
        val fixed = (warm + cool) * 60 + drills.sumOf { drillSeconds(it) }

        val baseSets = if (p.level == Level.BEGINNER || p.cautious) 2 else 3
        val rpe = when { p.cautious -> 6; p.level == Level.BEGINNER -> 7; else -> 8 }
        val focus = focusSlots(p.focus)
        val slots = ArrayList<Pair<Slot, Boolean>>() // slot, isMain
        tpl.main.forEach { slots += it to true }
        // Focus muscles get an extra slot first in line (only if relevant to the day).
        val dayHasRelated = { s: Slot -> tpl.main.contains(s) || tpl.extra.contains(s) || type.name.startsWith("FULL") }
        focus.distinct().filter(dayHasRelated).forEach { slots += it to false }
        tpl.extra.forEach { slots += it to false }

        val chosen = ArrayList<Pair<PlannedExercise, Boolean>>()
        val usedIds = HashSet<String>()
        for ((slot, isMain) in slots) {
            val ex = picker(slot, usedIds) ?: continue
            usedIds += ex.id
            val d = dose(p.goal, p.level, ex)
            var sets = if (isMain) baseSets else max(2, baseSets - 1)
            if (ex.primary.any { it in p.focus }) sets += 1
            var r = rpe
            if (deload) { sets = max(1, ceil(sets / 2.0).toInt()); r = max(5, rpe - 2) }
            chosen += PlannedExercise(ex.id, sets, d.repsMin, d.repsMax, d.rest, r, ex.timed) to isMain
        }

        fun total() = fixed + chosen.sumOf { exerciseSeconds(it.first) }

        // 1) drop extras from the end
        while (total() > budget && chosen.any { !it.second }) {
            val idx = chosen.indexOfLast { !it.second }
            chosen.removeAt(idx)
        }
        // 2) trim sets above 2 (from the end)
        while (total() > budget && chosen.any { it.first.sets > 2 }) {
            val idx = chosen.indexOfLast { it.first.sets > 2 }
            val (pe, m) = chosen[idx]; chosen[idx] = pe.copy(sets = pe.sets - 1) to m
        }
        // 3) shorten rests step by step (not below 60 s for compounds / 45 s otherwise)
        fun floorOf(pe: PlannedExercise) = if (Exercises.get(pe.exerciseId).slot in compound) 60 else 45
        while (total() > budget && chosen.any { it.first.restSec > floorOf(it.first) }) {
            for (i in chosen.indices) {
                val (pe, m) = chosen[i]
                chosen[i] = pe.copy(restSec = max(floorOf(pe), pe.restSec - 15)) to m
            }
        }
        // 4) drop lowest priority main slots, keep ≥ 3 exercises
        while (total() > budget && chosen.size > 3) {
            val idx = chosen.withIndex().minWith(compareBy({ Exercises.get(it.value.first.exerciseId).slot.priority }, { -it.index })).index
            chosen.removeAt(idx)
        }
        // 5) last resort: 1 set each (still better than nothing — ACSM 2026)
        while (total() > budget && chosen.any { it.first.sets > 1 }) {
            val idx = chosen.indexOfLast { it.first.sets > 1 }
            val (pe, m) = chosen[idx]; chosen[idx] = pe.copy(sets = pe.sets - 1) to m
        }
        // Spare time for muscle/strength goals: add sets to main compounds (up to 4).
        if (!deload && (p.goal == Goal.MUSCLE || p.goal == Goal.STRENGTH) && !p.cautious) {
            var changed = true
            while (changed) {
                changed = false
                for (i in chosen.indices) {
                    val (pe, m) = chosen[i]
                    if (!m || pe.sets >= 4) continue
                    val cand = pe.copy(sets = pe.sets + 1)
                    if (total() - exerciseSeconds(pe) + exerciseSeconds(cand) <= budget - 120) { chosen[i] = cand to m; changed = true }
                }
            }
        }

        val est = ceil(total() / 60.0).toInt()
        val title = when {
            p.goal == Goal.DEFENSE && drills.isNotEmpty() -> "${type.title} + навыки"
            else -> type.title
        }
        return Session(type, title, warm, chosen.map { it.first }, drills, 0, cool, est)
    }

    /** Same exercises as a normal week, roughly half the sets and RPE −2. */
    fun deloadOf(s: Session): Session {
        val ex = s.exercises.map { it.copy(sets = max(1, it.sets / 2), rpe = max(5, it.rpe - 2)) }
        val drills = s.drills.map { it.copy(rounds = max(1, it.rounds / 2)) }
        val sec = (s.warmupMin + s.cooldownMin) * 60 + ex.sumOf { exerciseSeconds(it) } + drills.sumOf { drillSeconds(it) }
        return s.copy(exercises = ex, drills = drills, estimatedMin = ceil(sec / 60.0).toInt())
    }

    fun buildSkillsSession(p: Profile, variant: Int, deload: Boolean): Session {
        val minutes = p.minutes.coerceIn(15, 120)
        val warm = warmupMin(minutes); val cool = cooldownMin(minutes)
        val budget = (minutes - warm - cool) * 60
        val list = ArrayList<PlannedDrill>()
        var used = 0
        for (d0 in skillsDrills(variant, p.level, p.cautious)) {
            val d = if (deload) d0.copy(rounds = max(1, d0.rounds / 2)) else d0
            val s = drillSeconds(d)
            if (used + s > budget) continue
            list += d; used += s
        }
        val title = if (variant % 2 == 0) "Навыки: стойка, удары, освобождения" else "Навыки: падения, вставание, уход"
        return Session(SKILLS, title, warm, emptyList(), list, 0, cool, warm + cool + ceil(used / 60.0).toInt())
    }

    fun buildCardioSession(p: Profile): Session {
        val minutes = p.minutes.coerceIn(15, 120)
        val warm = if (minutes < 25) 3 else 5
        val cool = warm
        val cardio = max(5, minutes - warm - cool)
        return Session(CARDIO, "Кардио (зона 2) и мобильность", warm, emptyList(), emptyList(), cardio, cool, warm + cardio + cool)
    }

    fun generate(p: Profile, weekIndex: Int = 0): WeekPlan {
        val days = p.days.distinct().filter { it in 1..7 }.sorted().ifEmpty { listOf(1, 3, 5) }
        val deload = isDeload(weekIndex)
        val types = dayTypes(p.goal, if (p.cautious) Level.BEGINNER else p.level, days.size)
        val pool = available(p.equipment, p.cautious)
        val picker = Picker(pool, targetDifficulty(p.level, p.cautious))
        var skillsVariant = 0
        var defVariant = 0
        val plans = days.zip(types).map { (day, type) ->
            val s = when (type) {
                SKILLS -> buildSkillsSession(p, skillsVariant++, deload)
                CARDIO -> buildCardioSession(p)
                else -> buildStrengthSession(type, p, { slot, ex -> picker.pick(slot, ex) }, false, defVariant++).let { if (deload) deloadOf(it) else it }
            }
            DayPlan(day, s)
        }
        val volume = setsPerMuscle(plans)
        val notes = buildList {
            add("Неделя ${weekIndex % DELOAD_EVERY + 1} из $DELOAD_EVERY в блоке" + if (deload) " — разгрузочная: меньше подходов, ниже RPE." else ".")
            if (days.size == 1) add("1 тренировка в неделю — это минимум. Для результата лучше 2–3 дня (ВОЗ, ACSM).")
            val low = lowFrequency(plans)
            if (days.size >= 2 && low.isNotEmpty()) add("Реже 2 раз в неделю: ${low.joinToString()}. Добавь день или минуты.")
            if (plans.any { d -> d.session.exercises.any { it.restSec < dose(p.goal, p.level, Exercises.get(it.exerciseId)).rest } }) add("Времени мало: отдых между подходами сокращён. Для силы лучше 2–3 минуты (Grgic 2018) — добавь минут, если можешь.")
            if (p.cautious) add("Осторожный режим (PAR-Q+): лёгкие варианты, RPE до 6. Посоветуйся с врачом.")
            if (p.goal == Goal.DEFENSE) add("Навыки самообороны — только безопасные соло-упражнения. Партнёрская работа — с тренером.")
            add("Аэробная нагрузка по ВОЗ: 150–300 мин в неделю. Ходьба и прогулки тоже считаются.")
        }
        return WeekPlan(plans, deload, weekIndex, volume, notes)
    }

    fun setsPerMuscle(plans: List<DayPlan>): Map<Muscle, Double> {
        val m = HashMap<Muscle, Double>()
        for (d in plans) for (pe in d.session.exercises) {
            val ex = Exercises.get(pe.exerciseId)
            if (ex.slot == Slot.CONDITIONING) continue
            ex.primary.forEach { m[it] = (m[it] ?: 0.0) + pe.sets }
            ex.secondary.forEach { m[it] = (m[it] ?: 0.0) + pe.sets * 0.5 }
        }
        return m
    }

    /** Number of sessions in which the muscle is trained as a primary mover. */
    fun frequency(plans: List<DayPlan>, muscle: Muscle): Int =
        plans.count { d -> d.session.exercises.any { muscle in Exercises.get(it.exerciseId).primary } }

    /** Key muscle groups that are trained as prime movers fewer than 2×/week. */
    fun lowFrequency(plans: List<DayPlan>): List<String> = buildList {
        if (plans.none { it.session.exercises.isNotEmpty() }) return@buildList
        listOf(Muscle.CHEST, Muscle.QUADS, Muscle.GLUTES).forEach { if (frequency(plans, it) < 2) add(it.title.lowercase()) }
        // Hip-hinge work (bridges, RDLs) loads the hamstrings even when they are listed as synergists.
        val ham = plans.count { d -> d.session.exercises.any { pe -> Exercises.get(pe.exerciseId).let { Muscle.HAMSTRINGS in it.primary || (it.slot == Slot.HINGE && Muscle.HAMSTRINGS in it.secondary) } } }
        if (ham < 2) add(Muscle.HAMSTRINGS.title.lowercase())
        val back = plans.count { d -> d.session.exercises.any { pe -> Exercises.get(pe.exerciseId).primary.any { it == Muscle.LATS || it == Muscle.UPPER_BACK } } }
        if (back < 2) add("спина")
    }

    fun drillName(id: String) = Skills.get(id).name
}
