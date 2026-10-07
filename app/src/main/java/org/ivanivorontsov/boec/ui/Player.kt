package org.ivanivorontsov.boec.ui

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ivanivorontsov.boec.alarm.AlarmScheduler
import org.ivanivorontsov.boec.alarm.Notifier
import org.ivanivorontsov.boec.content.Exercises
import org.ivanivorontsov.boec.content.Skills
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.Progression
import org.ivanivorontsov.boec.domain.ReminderFactory
import org.ivanivorontsov.boec.model.LogEntry
import org.ivanivorontsov.boec.model.PlannedDrill
import org.ivanivorontsov.boec.model.PlannedExercise
import org.ivanivorontsov.boec.model.Session
import org.ivanivorontsov.boec.util.Speaker

sealed class Step {
    abstract val title: String
    open val seconds: Int = 0
    data class Info(override val title: String, val subtitle: String, override val seconds: Int, val bullets: List<String>, val color: Long) : Step()
    data class Work(val pe: PlannedExercise, val set: Int, val total: Int, val exIndex: Int) : Step() {
        val ex get() = Exercises.get(pe.exerciseId)
        override val title get() = ex.name
        override val seconds get() = if (pe.timed) (pe.repsMin + pe.repsMax) / 2 * (if (ex.perSide) 2 else 1) else 0
    }
    data class DrillWork(val pd: PlannedDrill, val round: Int) : Step() {
        val drill get() = Skills.get(pd.drillId)
        override val title get() = drill.name
        override val seconds get() = if (pd.reps > 0) 0 else pd.workSec
    }
    data class Rest(override val seconds: Int, val next: String) : Step() { override val title get() = "Отдых" }
}

private val WARMUP = listOf(
    "1 мин — ходьба на месте или прыжки «звёздочка»",
    "Круги плечами и руками, 10 раз вперёд и назад",
    "Наклоны и повороты корпуса, «кошка-корова»",
    "Круги тазом, махи ногами вперёд-назад у опоры",
    "10 приседаний и 5 отжиманий от опоры — лёгкий подход",
)
private val COOLDOWN = listOf(
    "Спокойное дыхание: вдох 4 с, выдох 6 с",
    "Растяжка груди в дверном проёме — 30 с",
    "Растяжка задней поверхности бедра — по 30 с",
    "Растяжка сгибателей бедра в выпаде — по 30 с",
    "Поза ребёнка — 30–60 с",
)

fun buildSteps(s: Session): List<Step> {
    val out = ArrayList<Step>()
    if (s.warmupMin > 0) out += Step.Info("Разминка", "Разогрей тело и суставы", s.warmupMin * 60, WARMUP, 0xFFFFC145)
    s.exercises.forEachIndexed { ei, pe ->
        for (set in 1..pe.sets) {
            out += Step.Work(pe, set, pe.sets, ei)
            val next = if (set < pe.sets) "${Exercises.get(pe.exerciseId).name}, подход ${set + 1}" else s.exercises.getOrNull(ei + 1)?.let { Exercises.get(it.exerciseId).name } ?: s.drills.firstOrNull()?.let { Skills.get(it.drillId).name } ?: "заминка"
            out += Step.Rest(if (set < pe.sets) pe.restSec else minOf(pe.restSec, 90), next)
        }
    }
    s.drills.forEachIndexed { di, pd ->
        for (r in 1..pd.rounds) {
            out += Step.DrillWork(pd, r)
            if (pd.restSec > 0) {
                val next = if (r < pd.rounds) "${Skills.get(pd.drillId).name}, раунд ${r + 1}" else s.drills.getOrNull(di + 1)?.let { Skills.get(it.drillId).name } ?: "заминка"
                out += Step.Rest(pd.restSec, next)
            }
        }
    }
    if (s.cardioMin > 0) out += Step.Info("Кардио, зона 2", "Ходьба в темпе, лёгкий бег, велосипед", s.cardioMin * 60,
        listOf("Темп «могу говорить предложениями, но не петь»", "Ровное дыхание носом и ртом", "Это часть 150–300 минут в неделю по ВОЗ"), 0xFF5AB4FF)
    while (out.lastOrNull() is Step.Rest) out.removeAt(out.lastIndex)
    if (s.cooldownMin > 0) out += Step.Info("Заминка", "Растяжка без боли, дыхание", s.cooldownMin * 60, COOLDOWN, 0xFF3DDC97)
    return out
}

private fun spokenNumber(n: Int) = n.toString()

@Composable
fun PlayerScreen(session: Session) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val steps = remember(session) { buildSteps(session) }
    var index by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var stepStart by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var pausedAccum by remember { mutableLongStateOf(0L) }
    var pausedAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val sessionStart = remember { System.currentTimeMillis() }
    val reps = remember { mutableStateMapOf<Int, Int>() }         // step index -> reps done
    val rpe = remember { mutableStateMapOf<Int, Int>() }          // exercise index -> rpe
    var finished by remember { mutableStateOf(false) }
    var askExit by remember { mutableStateOf(false) }
    var pain by remember { mutableStateOf(false) }
    val speaker = remember { Speaker(ctx) { Store.value.settings.tts } }
    var lastSpokenSec by remember { mutableIntStateOf(-1) }

    DisposableEffect(Unit) {
        val w = (ctx as? android.app.Activity)?.window
        w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); speaker.shutdown() }
    }
    // Frame clock: time-based (elapsedRealtime), so the timer stays exact at 60/90/120/144 Hz and after pauses.
    LaunchedEffect(Unit) { while (true) withFrameMillis { now = SystemClock.elapsedRealtime() } }

    val step = steps.getOrNull(index)
    val elapsed = if (paused) pausedAt - stepStart - pausedAccum else now - stepStart - pausedAccum
    val remainingMs = ((step?.seconds ?: 0) * 1000L - elapsed).coerceAtLeast(0)

    fun go(to: Int) {
        if (to >= steps.size) { finished = true; speaker.say("Тренировка завершена. Отличная работа!"); return }
        index = to.coerceAtLeast(0); stepStart = SystemClock.elapsedRealtime(); pausedAccum = 0; paused = false; lastSpokenSec = -1
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    // Voice cue on step change.
    LaunchedEffect(index, finished) {
        if (finished) return@LaunchedEffect
        when (val st = steps.getOrNull(index)) {
            is Step.Info -> speaker.say("${st.title}. ${st.seconds / 60} минут.")
            is Step.Work -> speaker.say("Подход ${st.set} из ${st.total}. ${st.ex.name}. " + if (st.pe.timed) "${st.seconds} секунд." else "${st.pe.repsMin}–${st.pe.repsMax} повторов.")
            is Step.DrillWork -> speaker.say("${st.drill.name}. " + if (st.pd.reps > 0) "${st.pd.reps} повторов." else "${st.pd.workSec} секунд.")
            is Step.Rest -> speaker.say("Отдых ${st.seconds} секунд. Далее: ${st.next}.")
            null -> {}
        }
    }
    // Countdown cues + auto-advance for timed steps.
    val secLeft = ((remainingMs + 999) / 1000).toInt()
    LaunchedEffect(secLeft, index, paused) {
        val st = step ?: return@LaunchedEffect
        if (st.seconds <= 0 || paused || finished) return@LaunchedEffect
        if (secLeft != lastSpokenSec) {
            lastSpokenSec = secLeft
            if (secLeft == 10 && st.seconds >= 30) speaker.say("Осталось десять секунд")
            if (secLeft in 1..3) speaker.say(spokenNumber(secLeft))
        }
        if (remainingMs <= 0L) go(index + 1)
    }

    fun save(completed: Boolean, stoppedForPain: Boolean) {
        val works = steps.withIndex().filter { it.value is Step.Work && it.index < (if (finished) steps.size else index + 1) }
        val avg = if (rpe.isEmpty()) 0.0 else rpe.values.average()
        val entry = LogEntry(Store.todayEpochDay(), sessionStart, session.title, session.type,
            ((System.currentTimeMillis() - sessionStart) / 1000).toInt(), works.size, avg, completed, stoppedForPain)
        val results = HashMap<String, String>()
        session.exercises.forEachIndexed { ei, pe ->
            val r = steps.withIndex().filter { (it.value as? Step.Work)?.exIndex == ei }.mapNotNull { reps[it.index] }
            if (r.isNotEmpty()) results[pe.exerciseId] = r.joinToString(",") + ";" + (rpe[ei] ?: 0)
        }
        val todayIso = Dates.isoDay(Store.todayEpochDay())
        Store.update { s -> s.copy(log = s.log + entry, lastResults = s.lastResults + results, snoozes = s.snoozes.filter { it.reminderId != ReminderFactory.workoutId(todayIso) }) }
        Notifier.cancel(ctx, ReminderFactory.workoutId(todayIso))
        AlarmScheduler.rescheduleAll(ctx)
    }

    BackHandler { askExit = true }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (finished) nav.pop() else askExit = true }) { Icon(Icons.Default.Close, "Закрыть") }
            Column(Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(if (finished) "Готово" else "Шаг ${index + 1} из ${steps.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton({ pain = true }, colors = ButtonDefaults.textButtonColors(contentColor = Danger)) { Text("Боль — стоп", fontWeight = FontWeight.Bold) }
        }
        val prog by animateFloatAsState(if (finished) 1f else index / steps.size.coerceAtLeast(1).toFloat(), spring(stiffness = Spring.StiffnessLow), label = "prog")
        LinearProgressIndicator(progress = { prog }, Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(RoundedCornerShape(50)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), gapSize = 0.dp, drawStopIndicator = {})

        if (finished) {
            Summary(session, steps, reps, rpe, sessionStart) { save(true, false); nav.pop() }
            return@Column
        }
        if (step == null) return@Column

        AnimatedContent(index, Modifier.weight(1f), transitionSpec = {
            (slideInVertically(tween(320)) { it / 10 } + fadeIn(tween(260)) + scaleIn(tween(320), 0.96f)) togetherWith (slideOutVertically(tween(200)) { -it / 10 } + fadeOut(tween(160)))
        }, label = "step") { i ->
            val st = steps[i]
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(16.dp))
                val accent = when (st) { is Step.Rest -> Sky; is Step.Info -> Color(st.color); is Step.DrillWork -> Mint; is Step.Work -> Ember }
                Pill(when (st) {
                    is Step.Work -> "Подход ${st.set} из ${st.total}"
                    is Step.DrillWork -> "Раунд ${st.round} из ${st.pd.rounds}"
                    is Step.Rest -> "Восстановление"
                    is Step.Info -> st.subtitle
                }, accent)
                Spacer(Modifier.height(10.dp))
                Text(st.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                if (st.seconds > 0) {
                    TimerRing(totalMs = st.seconds * 1000L, remaining = { if (i == index) remainingMs else st.seconds * 1000L }, color = accent, paused = paused)
                } else if (st is Step.Work) {
                    RepsBlock(st, reps[i] ?: st.pe.repsMax) { reps[i] = it }
                } else if (st is Step.DrillWork) {
                    Text("${st.pd.reps}", style = MaterialTheme.typography.displayLarge.merge(MonoDigits), color = accent)
                    Text("повторов" + if (st.drill.needsMat) " · на мягком полу" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(18.dp))
                when (st) {
                    is Step.Work -> {
                        if (st.seconds > 0) Text("Цель: RPE ${st.pe.rpe} · держи технику", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BCard(Modifier.fillMaxWidth(), padding = 14.dp) { Bullets(st.ex.cues.take(3), Ember) }
                        if (st.set == st.total) {
                            Spacer(Modifier.height(10.dp))
                            Text("Насколько тяжело (RPE)?", style = MaterialTheme.typography.titleSmall)
                            ChoiceChips((6..10).toList(), { rpe[st.exIndex] == it }, { "$it" }) { rpe[st.exIndex] = it }
                            Text("10 — больше ни одного повтора, 8 — оставалось 2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is Step.DrillWork -> BCard(Modifier.fillMaxWidth(), padding = 14.dp) { Bullets(st.drill.how, Mint); Spacer(Modifier.height(8.dp)); Bullets(st.drill.safety, Amber) }
                    is Step.Rest -> {
                        Text("Далее: ${st.next}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ pausedAccum += 15_000 }, shape = RoundedCornerShape(12.dp)) { Text("+15 с") }
                            OutlinedButton({ go(index + 1) }, shape = RoundedCornerShape(12.dp)) { Text("Пропустить отдых") }
                        }
                    }
                    is Step.Info -> BCard(Modifier.fillMaxWidth(), padding = 14.dp) { Bullets(st.bullets, Color(st.color)) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledIconButton({ if (index > 0) go(index - 1) }, Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(Icons.Default.KeyboardArrowLeft, "Назад")
            }
            val main = when {
                step.seconds > 0 -> if (paused) "Продолжить" else "Пауза"
                step is Step.Work -> "Подход выполнен"
                else -> "Готово"
            }
            Button(
                onClick = {
                    if (step.seconds > 0) {
                        if (paused) { pausedAccum += SystemClock.elapsedRealtime() - pausedAt; paused = false } else { pausedAt = SystemClock.elapsedRealtime(); paused = true }
                    } else {
                        if (step is Step.Work && reps[index] == null) reps[index] = step.pe.repsMax
                        go(index + 1)
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(18.dp),
            ) {
                Icon(if (step.seconds > 0 && !paused) AppIcons.Timer else if (step.seconds > 0) Icons.Default.PlayArrow else Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp)); Text(main, style = MaterialTheme.typography.titleMedium)
            }
            FilledIconButton({ go(index + 1) }, Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(Icons.Default.KeyboardArrowRight, "Дальше")
            }
        }
    }

    if (askExit) AlertDialog(
        onDismissRequest = { askExit = false },
        title = { Text("Завершить тренировку?") },
        text = { Text("Можно сохранить как частично выполненную — она попадёт в историю.") },
        confirmButton = { TextButton({ save(false, false); askExit = false; nav.pop() }) { Text("Сохранить и выйти") } },
        dismissButton = { Row { TextButton({ askExit = false; nav.pop() }) { Text("Не сохранять") }; TextButton({ askExit = false }) { Text("Продолжить") } } },
    )
    if (pain) AlertDialog(
        onDismissRequest = { pain = false },
        title = { Text("Остановись", color = Danger) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Острая, колющая или нарастающая боль — сигнал прекратить упражнение. Не «терпи».")
                Text("Боль или давление в груди, сильная одышка, головокружение, онемение, нарушение речи — немедленно звони 112.", color = Danger, fontWeight = FontWeight.SemiBold)
                Text("Лёгкая мышечная усталость и жжение — норма, а боль в суставе — нет. При повторяющейся боли обратись к врачу.")
            }
        },
        confirmButton = { TextButton({ speaker.say("Тренировка остановлена"); save(false, true); pain = false; nav.pop() }) { Text("Завершить тренировку") } },
        dismissButton = { TextButton({ pain = false; go(index + 1) }) { Text("Пропустить упражнение") } },
    )
}

@Composable
private fun TimerRing(totalMs: Long, remaining: () -> Long, color: Color, paused: Boolean) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val sec = ((remaining() + 999) / 1000).toInt()
    val pulse by animateFloatAsState(if (paused) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "pulse")
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            // Read the time inside draw: only this layer redraws each frame.
            val frac = (remaining().toFloat() / totalMs).coerceIn(0f, 1f)
            drawArc(color, -90f, 360f * frac * pulse, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmtMin(sec), style = MaterialTheme.typography.displayMedium.merge(MonoDigits).copy(fontSize = 56.sp))
            Text(if (paused) "пауза" else "осталось", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RepsBlock(st: Step.Work, value: Int, onChange: (Int) -> Unit) {
    Text("${st.pe.repsMin}–${st.pe.repsMax}", style = MaterialTheme.typography.displayLarge.merge(MonoDigits), color = Ember)
    Text((if (st.ex.perSide) "повторов на каждую сторону" else "повторов") + " · RPE ${st.pe.rpe} (в запасе ${10 - st.pe.rpe})", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton({ onChange((value - 1).coerceAtLeast(0)) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("−", fontSize = 22.sp) }
        Text("  сделал: $value  ", style = MaterialTheme.typography.titleLarge)
        FilledIconButton({ onChange(value + 1) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface)) { Text("+", fontSize = 22.sp) }
    }
}

@Composable
private fun Summary(session: Session, steps: List<Step>, reps: Map<Int, Int>, rpe: Map<Int, Int>, start: Long, onSave: () -> Unit) {
    val scale by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "s")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val sc by animateFloatAsState(if (shown) 1f else 0.4f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sc")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(110.dp * sc * scale).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(60.dp * sc))
        }
        Spacer(Modifier.height(16.dp))
        Text("Тренировка завершена!", style = MaterialTheme.typography.headlineMedium)
        Text("${(System.currentTimeMillis() - start) / 60000} мин · ${steps.count { it is Step.Work }} подходов", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        if (session.exercises.isNotEmpty()) BCard(Modifier.fillMaxWidth()) {
            Text("Что дальше (двойная прогрессия)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            session.exercises.forEachIndexed { ei, pe ->
                val done = steps.withIndex().filter { (it.value as? Step.Work)?.exIndex == ei }.mapNotNull { reps[it.index] }
                Text(Exercises.get(pe.exerciseId).name, style = MaterialTheme.typography.titleSmall)
                Text(Progression.advice(pe, done, rpe[ei]), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onSave, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Сохранить в историю") }
    }
}
