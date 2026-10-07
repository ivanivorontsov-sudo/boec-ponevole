package org.ivanivorontsov.boec

import org.ivanivorontsov.boec.content.Exercises
import org.ivanivorontsov.boec.content.Skills
import org.ivanivorontsov.boec.domain.ScheduleGenerator
import org.ivanivorontsov.boec.model.Equipment
import org.ivanivorontsov.boec.model.Goal
import org.ivanivorontsov.boec.model.Level
import org.ivanivorontsov.boec.model.Muscle
import org.ivanivorontsov.boec.model.Profile
import org.ivanivorontsov.boec.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleGeneratorTest {
    private val goals = Goal.entries
    private val levels = Level.entries
    private val dayCombos = listOf(listOf(1), listOf(1, 4), listOf(1, 3, 5), listOf(1, 2, 4, 5), listOf(1, 2, 3, 5, 6), listOf(1, 2, 3, 4, 5, 6), (1..7).toList())
    private val minutesList = listOf(15, 20, 30, 45, 60, 90)

    private fun all(): Sequence<Profile> = sequence {
        for (g in goals) for (l in levels) for (d in dayCombos) for (m in minutesList) for (eq in listOf(listOf(Equipment.NONE), Equipment.entries.toList()))
            for (c in listOf(false, true)) yield(Profile(goal = g, level = l, days = d, minutes = m, equipment = eq, cautious = c))
    }

    @Test fun everyPlannedDayIsOneOfTheUsersDays() {
        all().forEach { p ->
            val plan = ScheduleGenerator.generate(p)
            assertEquals(p.days.sorted(), plan.days.map { it.isoDay })
        }
    }

    @Test fun sessionsFitIntoAvailableTime() {
        all().forEach { p ->
            ScheduleGenerator.generate(p).days.forEach { d ->
                // allow 1 min rounding
                assertTrue("${p.goal} ${p.days} ${p.minutes}: ${d.session.title} took ${d.session.estimatedMin}", d.session.estimatedMin <= p.minutes + 1)
            }
        }
    }

    @Test fun strengthSessionsHaveAtLeastThreeExercises() {
        all().forEach { p ->
            ScheduleGenerator.generate(p).days.filter { it.session.type !in setOf(SessionType.SKILLS, SessionType.CARDIO) }.forEach {
                assertTrue("${p}: ${it.session}", it.session.exercises.size >= 3)
            }
        }
    }

    @Test fun majorMusclesTrainedTwicePerWeekWithTwoOrMoreDays() {
        all().filter { it.days.size >= 2 && it.minutes >= 30 }.forEach { p ->
            val plan = ScheduleGenerator.generate(p)
            assertTrue("$p -> ${ScheduleGenerator.lowFrequency(plan.days)}", ScheduleGenerator.lowFrequency(plan.days).isEmpty())
        }
    }

    @Test fun hypertrophyGoalReachesTenWeeklySetsForChestAndQuadsWhenTimeAllows() {
        val p = Profile(goal = Goal.MUSCLE, level = Level.INTERMEDIATE, days = listOf(1, 2, 4, 5), minutes = 60, equipment = Equipment.entries.toList())
        val v = ScheduleGenerator.generate(p).setsPerMuscle
        assertTrue("chest ${v[Muscle.CHEST]}", (v[Muscle.CHEST] ?: 0.0) >= 10)
        assertTrue("quads ${v[Muscle.QUADS]}", (v[Muscle.QUADS] ?: 0.0) >= 10)
    }

    @Test fun onlyAvailableEquipmentIsUsed() {
        val p = Profile(days = listOf(1, 3, 5), equipment = listOf(Equipment.NONE))
        ScheduleGenerator.generate(p).days.flatMap { it.session.exercises }.forEach {
            assertEquals(Equipment.NONE, Exercises.get(it.exerciseId).equipment)
        }
    }

    @Test fun noDuplicateExerciseInOneSession() {
        all().forEach { p ->
            ScheduleGenerator.generate(p).days.forEach { d ->
                val ids = d.session.exercises.map { it.exerciseId }
                assertEquals(ids.size, ids.toSet().size)
            }
        }
    }

    @Test fun cautiousModeAvoidsRiskyExercisesAndKeepsLowEffort() {
        val p = Profile(goal = Goal.DEFENSE, level = Level.BEGINNER, days = listOf(1, 2, 3, 4, 5), minutes = 60, equipment = Equipment.entries.toList(), cautious = true)
        val plan = ScheduleGenerator.generate(p)
        val ids = plan.days.flatMap { it.session.exercises }.map { it.exerciseId }
        assertFalse(ids.any { it in setOf("vp_pike", "vp_pike_elev", "cd_burpee", "cd_sprint", "hi_nordic") })
        assertTrue(plan.days.flatMap { it.session.exercises }.all { it.rpe <= 6 })
        assertFalse(plan.days.flatMap { it.session.drills }.any { it.drillId == "cd_sprints" })
    }

    @Test fun deloadEveryFifthWeekHalvesVolume() {
        val p = Profile(goal = Goal.MUSCLE, level = Level.INTERMEDIATE, days = listOf(1, 3, 5), minutes = 60)
        val normal = ScheduleGenerator.generate(p, 0)
        val deload = ScheduleGenerator.generate(p, 4)
        assertFalse(normal.deload); assertTrue(deload.deload)
        val n = normal.days.sumOf { d -> d.session.exercises.sumOf { it.sets } }
        val dl = deload.days.sumOf { d -> d.session.exercises.sumOf { it.sets } }
        assertTrue("normal $n deload $dl", dl <= n * 0.6)
        assertTrue(ScheduleGenerator.isDeload(9)); assertFalse(ScheduleGenerator.isDeload(5))
    }

    @Test fun defenseGoalIncludesSkillsAndRestBetweenStrengthDays() {
        val p = Profile(goal = Goal.DEFENSE, days = listOf(1, 3, 5), minutes = 45)
        val types = ScheduleGenerator.generate(p).days.map { it.session.type }
        assertEquals(listOf(SessionType.FULL_A, SessionType.SKILLS, SessionType.FULL_B), types)
        val skills = ScheduleGenerator.generate(p).days[1].session
        assertTrue(skills.drills.isNotEmpty())
        skills.drills.forEach { Skills.get(it.drillId) } // all drill ids exist
    }

    @Test fun focusMuscleGetsMoreVolume() {
        val base = Profile(goal = Goal.HEALTH, days = listOf(1, 3, 5), minutes = 60, equipment = Equipment.entries.toList())
        val focused = base.copy(focus = listOf(Muscle.SHOULDERS))
        val a = ScheduleGenerator.generate(base).setsPerMuscle[Muscle.SHOULDERS] ?: 0.0
        val b = ScheduleGenerator.generate(focused).setsPerMuscle[Muscle.SHOULDERS] ?: 0.0
        assertTrue("$a -> $b", b > a)
    }

    @Test fun libraryIsConsistent() {
        Exercises.all.forEach { e ->
            e.easier?.let { Exercises.get(it) }
            e.harder?.let { Exercises.get(it) }
            assertTrue(e.id, e.cues.isNotEmpty() && e.mistakes.isNotEmpty() && e.contra.isNotEmpty())
        }
        assertEquals(Exercises.all.size, Exercises.all.map { it.id }.toSet().size)
    }
}
