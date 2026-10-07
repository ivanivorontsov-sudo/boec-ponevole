package org.ivanivorontsov.boec.model

import kotlinx.serialization.Serializable

@Serializable
enum class Goal(val title: String, val subtitle: String) {
    HEALTH("Сила и здоровье", "Все мышцы 2–3 раза в неделю, умеренная нагрузка"),
    MUSCLE("Мышцы", "Больше объёма: 10–20 подходов на мышцу в неделю"),
    STRENGTH("Сила", "Меньше повторов, тяжелее варианты, длинный отдых"),
    DEFENSE("Самооборона", "Сила + навыки + выносливость, чтобы уйти и выстоять"),
}

@Serializable
enum class Level(val title: String) { BEGINNER("Новичок"), INTERMEDIATE("Средний") }

@Serializable
enum class Equipment(val title: String) {
    NONE("Без инвентаря"), DUMBBELLS("Гантели"), BAR("Турник"), BAND("Резинка-эспандер")
}

@Serializable
enum class Muscle(val title: String, val major: Boolean = true) {
    CHEST("Грудные"),
    LATS("Широчайшие"),
    UPPER_BACK("Верх спины"),
    SHOULDERS("Плечи (дельты)"),
    BICEPS("Бицепс", false),
    TRICEPS("Трицепс", false),
    FOREARMS("Предплечья и хват", false),
    CORE("Пресс и кор"),
    LOWER_BACK("Поясница", false),
    GLUTES("Ягодицы"),
    QUADS("Квадрицепсы"),
    HAMSTRINGS("Задняя поверхность бедра"),
    CALVES("Икры", false),
    NECK("Шея", false),
}

/** Movement pattern a session needs; the generator picks a concrete exercise for each slot. */
@Serializable
enum class Slot(val title: String, val priority: Int) {
    SQUAT("Присед", 10), HINGE("Наклон / таз", 10), LUNGE("Выпады", 9),
    HPUSH("Жим горизонтальный", 10), VPUSH("Жим вверх", 7),
    HPULL("Тяга горизонтальная", 10), VPULL("Тяга вертикальная", 8),
    CORE("Кор", 6), CALF("Икры", 3), BICEPS("Бицепс", 3), TRICEPS("Трицепс", 3),
    GRIP("Хват", 4), NECK("Шея", 4), CONDITIONING("Кондиции", 5),
}

data class Exercise(
    val id: String,
    val name: String,
    val slot: Slot,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
    val equipment: Equipment = Equipment.NONE,
    /** 1 = easiest … 5 = hardest. */
    val difficulty: Int,
    /** If true the "reps" are seconds (holds, intervals). */
    val timed: Boolean = false,
    val perSide: Boolean = false,
    val cues: List<String>,
    val mistakes: List<String>,
    val contra: List<String>,
    val easier: String? = null,
    val harder: String? = null,
)

@Serializable
data class Profile(
    val name: String = "",
    val goal: Goal = Goal.DEFENSE,
    val level: Level = Level.BEGINNER,
    /** ISO day numbers 1 = Monday … 7 = Sunday. */
    val days: List<Int> = listOf(1, 3, 5),
    val hour: Int = 19,
    val minute: Int = 0,
    val minutes: Int = 40,
    val equipment: List<Equipment> = listOf(Equipment.NONE),
    val focus: List<Muscle> = emptyList(),
    /** PAR-Q+ flagged: keep intensity low until a doctor clears the user. */
    val cautious: Boolean = false,
)

@Serializable
enum class SessionType(val title: String) {
    FULL_A("Всё тело A"), FULL_B("Всё тело B"), FULL_C("Всё тело C"),
    UPPER("Верх тела"), LOWER("Низ тела"),
    PUSH("Жимы"), PULL("Тяги"), LEGS("Ноги"),
    SKILLS("Навыки самообороны и кондиции"),
    CARDIO("Кардио и мобильность"),
}

@Serializable
data class PlannedExercise(
    val exerciseId: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restSec: Int,
    /** Target RPE (10 = отказ, 8 = 2 повтора в запасе). */
    val rpe: Int,
    val timed: Boolean = false,
)

@Serializable
data class PlannedDrill(val drillId: String, val rounds: Int, val workSec: Int, val restSec: Int, val reps: Int = 0)

@Serializable
data class Session(
    val type: SessionType,
    val title: String,
    val warmupMin: Int,
    val exercises: List<PlannedExercise>,
    val drills: List<PlannedDrill> = emptyList(),
    val cardioMin: Int = 0,
    val cooldownMin: Int,
    val estimatedMin: Int,
)

@Serializable
data class DayPlan(val isoDay: Int, val session: Session)

@Serializable
data class WeekPlan(
    val days: List<DayPlan>,
    val deload: Boolean,
    val weekIndex: Int,
    /** Weekly hard sets per muscle (primary = 1, secondary = 0.5). */
    val setsPerMuscle: Map<Muscle, Double>,
    val notes: List<String>,
)

@Serializable
enum class ReminderKind(val title: String) {
    WORKOUT("Тренировка"), SKILL("Навыки"), WATER("Вода"), POSTURE("Осанка и перерыв"),
    SLEEP("Сон"), STRETCH("Растяжка"), TICKS("Осмотр на клещей"), FIRST_AID("Аптечка"),
    EYES("Глаза 20-20-20"), CUSTOM("Своё"),
}

@Serializable
enum class AlertMode(val title: String) { NOTIFICATION("Уведомление"), CALL("Звонок") }

@Serializable
data class Reminder(
    val id: String,
    val kind: ReminderKind,
    val title: String,
    val text: String,
    val enabled: Boolean = true,
    val mode: AlertMode = AlertMode.NOTIFICATION,
    /** Bit 0 = Monday … bit 6 = Sunday. */
    val daysMask: Int = 0b1111111,
    val hour: Int,
    val minute: Int,
    /** >0: repeat every N minutes from hour:minute until untilHour:untilMinute (inclusive). */
    val repeatEveryMin: Int = 0,
    val untilHour: Int = 0,
    val untilMinute: Int = 0,
    /** Bit 0 = January … bit 11 = December. */
    val monthsMask: Int = 0xFFF,
    /** >0: only on this day of month (monthly reminders); daysMask is ignored. */
    val dayOfMonth: Int = 0,
    /** For workout reminders: which ISO day of the plan it belongs to. */
    val planDay: Int = 0,
)

@Serializable
data class Snooze(val reminderId: String, val atMillis: Long, val count: Int = 0)

@Serializable
data class LogEntry(
    val epochDay: Long,
    val startedAt: Long,
    val title: String,
    val type: SessionType,
    val durationSec: Int,
    val sets: Int,
    val avgRpe: Double = 0.0,
    val completed: Boolean = true,
    val stoppedForPain: Boolean = false,
)

@Serializable
enum class ThemeMode(val title: String) { SYSTEM("Как в системе"), DARK("Тёмная"), LIGHT("Светлая") }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val tts: Boolean = true,
    val snoozeMin: Int = 10,
    val callTimeoutSec: Int = 60,
    val vibrate: Boolean = true,
)

@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val parq: List<Boolean> = emptyList(),
    val profile: Profile = Profile(),
    /** Epoch day of the Monday when the current training block started (for deload weeks). */
    val blockStartEpochDay: Long = 0,
    val reminders: List<Reminder> = emptyList(),
    val snoozes: List<Snooze> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val settings: Settings = Settings(),
    /** Last completed exercise result: exerciseId -> "reps;rpe" used for progression hints. */
    val lastResults: Map<String, String> = emptyMap(),
)
