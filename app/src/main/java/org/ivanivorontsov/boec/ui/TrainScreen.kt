package org.ivanivorontsov.boec.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ivanivorontsov.boec.content.Exercises
import org.ivanivorontsov.boec.content.Science
import org.ivanivorontsov.boec.content.Skills
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.ReminderMath
import org.ivanivorontsov.boec.model.DayPlan
import org.ivanivorontsov.boec.model.Equipment
import org.ivanivorontsov.boec.model.Exercise
import org.ivanivorontsov.boec.model.Goal
import org.ivanivorontsov.boec.model.Muscle
import org.ivanivorontsov.boec.model.PlannedExercise

@Composable
fun TrainScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Header("Тренировки", "План, упражнения и наука")
        SegmentedTabs(listOf("План", "Упражнения", "Наука"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "train") { t ->
            when (t) { 0 -> PlanTab(); 1 -> LibraryTab(); else -> ScienceTab() }
        }
    }
}

@Composable
private fun PlanTab() {
    val nav = LocalNav.current
    val s by Store.state.collectAsState()
    val plan = remember(s.profile, s.blockStartEpochDay) { Store.plan(s) }
    val todayIso = Dates.isoDay(Store.todayEpochDay())
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BCard(Modifier.fillMaxWidth(), onClick = { nav.push(Route.EditProfile) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.profile.goal.title + " · " + s.profile.level.title, style = MaterialTheme.typography.titleLarge)
                        Text("${s.profile.days.size} дн/нед · ${s.profile.minutes} мин · звонок в %02d:%02d".format(s.profile.hour, s.profile.minute), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s.profile.equipment.filter { it != Equipment.NONE }.joinToString { it.title }.ifEmpty { "Без инвентаря" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ nav.push(Route.EditProfile) }) { Icon(Icons.Default.Edit, "Изменить") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Неделя ${plan.weekIndex % 5 + 1}/5", MaterialTheme.colorScheme.primary)
                    if (plan.deload) Pill("Разгрузка", Mint)
                    if (s.profile.cautious) Pill("Осторожный режим", Amber)
                }
            }
        }
        items(7) { i ->
            val iso = i + 1
            val d = plan.days.firstOrNull { it.isoDay == iso }
            DayCard(iso, d, iso == todayIso) { d?.let { nav.openPlayer(it.session) } }
        }
        item { VolumeCard(plan.setsPerMuscle, s.profile.goal) }
        item {
            BCard(Modifier.fillMaxWidth()) {
                Text("Почему так", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Bullets(plan.notes)
            }
        }
    }
}

@Composable
private fun DayCard(iso: Int, d: DayPlan?, today: Boolean, onStart: () -> Unit) {
    val nav = LocalNav.current
    var open by rememberSaveable(iso) { mutableStateOf(today) }
    BCard(
        Modifier.fillMaxWidth().animateContentSize(), onClick = if (d != null) ({ open = !open }) else null,
        color = if (today) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh, padding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (d != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(ReminderMath.DAY_SHORT[iso - 1], color = if (d != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d?.session?.title ?: "Отдых / прогулка", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (d == null) "Восстановление: сон, ходьба, лёгкая растяжка"
                    else "≈${d.session.estimatedMin} мин" + (if (d.session.exercises.isNotEmpty()) " · ${d.session.exercises.size} упр." else "") + (if (d.session.drills.isNotEmpty()) " · ${d.session.drills.size} навыков" else "") + (if (d.session.cardioMin > 0) " · кардио ${d.session.cardioMin} мин" else ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (today && d != null) Pill("сегодня")
        }
        AnimatedVisibility(open && d != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column {
                Spacer(Modifier.height(12.dp))
                d!!.session.exercises.forEach { pe -> ExerciseLine(pe) { nav.push(Route.ExerciseDetail(pe.exerciseId)) } }
                d.session.drills.forEach { pd ->
                    val dr = Skills.get(pd.drillId)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { nav.push(Route.DrillDetail(dr.id)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Mint)); Spacer(Modifier.width(10.dp))
                        Text(dr.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(if (pd.reps > 0) "${pd.rounds}×${pd.reps}" else "${pd.rounds}×${fmtMin(pd.workSec)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (d.session.cardioMin > 0) Text("Кардио зона 2: ${d.session.cardioMin} мин — ходьба в темпе, бег трусцой, велосипед. Можешь говорить предложениями.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onStart, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Начать") }
            }
        }
    }
}

@Composable
fun ExerciseLine(pe: PlannedExercise, onClick: () -> Unit) {
    val ex = Exercises.get(pe.exerciseId)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.name, style = MaterialTheme.typography.bodyMedium)
            Text("отдых ${fmtMin(pe.restSec)} · RPE ${pe.rpe}" + if (ex.perSide) " · на каждую сторону" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${pe.sets}×${pe.repsMin}–${pe.repsMax}${if (pe.timed) " с" else ""}", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun VolumeCard(volume: Map<Muscle, Double>, goal: Goal) {
    val target = if (goal == Goal.MUSCLE) 10.0 else 6.0
    val max = (volume.values.maxOrNull() ?: 1.0).coerceAtLeast(target + 2)
    BCard(Modifier.fillMaxWidth()) {
        Text("Подходов в неделю по мышцам", style = MaterialTheme.typography.titleMedium)
        Text("Линия — ориентир ${target.toInt()} подходов" + if (goal == Goal.MUSCLE) " (ACSM 2026: ≈10+ для роста мышц)" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Muscle.entries.filter { (volume[it] ?: 0.0) > 0 }.sortedByDescending { volume[it] }.forEach { m ->
            val v = volume[m] ?: 0.0
            val frac by animateFloatAsState((v / max).toFloat(), tween(700), label = "vol")
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.title, Modifier.width(130.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(frac).height(12.dp).clip(RoundedCornerShape(6.dp)).background(if (v >= target) Mint else MaterialTheme.colorScheme.primary))
                    Box(Modifier.fillMaxWidth((target / max).toFloat()).height(12.dp)) { Box(Modifier.align(Alignment.CenterEnd).width(2.dp).height(12.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) }
                }
                Text(" %.0f".format(v), Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LibraryTab() {
    val nav = LocalNav.current
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val list = remember(filter) { Exercises.all.filter { f -> filter == null || f.primary.any { it.name == filter } || f.secondary.any { it.name == filter } }.sortedBy { it.difficulty } }
    Column {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(filter == null, { filter = null }, { Text("Все") }, shape = RoundedCornerShape(12.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)) }
            items(Muscle.entries) { m -> FilterChip(filter == m.name, { filter = if (filter == m.name) null else m.name }, { Text(m.title) }, shape = RoundedCornerShape(12.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)) }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { ex -> ExerciseCard(ex, Modifier.animateItem()) { nav.push(Route.ExerciseDetail(ex.id)) } }
        }
    }
}

@Composable
private fun ExerciseCard(ex: Exercise, modifier: Modifier, onClick: () -> Unit) {
    BCard(modifier.fillMaxWidth(), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                Text(ex.primary.joinToString { it.title }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (ex.equipment != Equipment.NONE) Pill(ex.equipment.title, Sky)
                    Difficulty(ex.difficulty)
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Difficulty(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { i -> Box(Modifier.size(width = 12.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) }
    }
}

@Composable
private fun ScienceTab() {
    val nav = LocalNav.current
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Science.principles) { InfoCardView(it, initiallyOpen = it == Science.principles.first()) }
        item { OutlinedButton({ nav.push(Route.Sources) }, Modifier.fillMaxWidth()) { Text("Все источники") } }
    }
}

@Composable
fun ExerciseDetailScreen(id: String) {
    val nav = LocalNav.current
    val ex = Exercises.get(id)
    Column(Modifier.fillMaxSize()) {
        Header(ex.name, onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BCard(Modifier.fillMaxWidth()) {
                KeyValue("Основные мышцы", ex.primary.joinToString { it.title })
                if (ex.secondary.isNotEmpty()) KeyValue("Помогают", ex.secondary.joinToString { it.title })
                KeyValue("Инвентарь", ex.equipment.title)
                KeyValue("Формат", if (ex.timed) "на время" else "повторы" + if (ex.perSide) ", каждая сторона" else "")
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text("Сложность  ", color = MaterialTheme.colorScheme.onSurfaceVariant); Difficulty(ex.difficulty) }
            }
            BCard(Modifier.fillMaxWidth()) { Text("Техника", style = MaterialTheme.typography.titleMedium, color = Mint); Spacer(Modifier.height(8.dp)); Bullets(ex.cues, Mint) }
            BCard(Modifier.fillMaxWidth()) { Text("Частые ошибки", style = MaterialTheme.typography.titleMedium, color = Amber); Spacer(Modifier.height(8.dp)); Bullets(ex.mistakes, Amber) }
            BCard(Modifier.fillMaxWidth(), color = Danger.copy(alpha = 0.10f)) {
                Text("Противопоказания и осторожность", style = MaterialTheme.typography.titleMedium, color = Danger); Spacer(Modifier.height(8.dp))
                Bullets(ex.contra + "Острая или нарастающая боль — прекрати упражнение.", Danger)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ex.easier?.let { e -> OutlinedButton({ nav.push(Route.ExerciseDetail(e)) }, Modifier.weight(1f)) { Text("Проще: " + Exercises.get(e).name, maxLines = 2) } }
                ex.harder?.let { h -> OutlinedButton({ nav.push(Route.ExerciseDetail(h)) }, Modifier.weight(1f)) { Text("Сложнее: " + Exercises.get(h).name, maxLines = 2) } }
            }
        }
    }
}

@Composable
fun EditProfileScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    var p by remember { mutableStateOf(s.profile) }
    Column(Modifier.fillMaxSize()) {
        Header("Параметры плана", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            ProfileForm(p) { p = it }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = { Actions.applyProfile(ctx, p, restartBlock = p.goal != s.profile.goal || p.level != s.profile.level); nav.pop() },
            enabled = p.days.isNotEmpty(), modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp), shape = RoundedCornerShape(16.dp),
        ) { Text("Сохранить и перестроить план") }
    }
}
