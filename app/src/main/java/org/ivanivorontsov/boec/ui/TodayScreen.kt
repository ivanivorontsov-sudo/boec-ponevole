package org.ivanivorontsov.boec.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ivanivorontsov.boec.alarm.AlarmScheduler
import org.ivanivorontsov.boec.content.Science
import org.ivanivorontsov.boec.content.Skills
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.ReminderMath
import org.ivanivorontsov.boec.domain.Streaks
import org.ivanivorontsov.boec.model.PlannedDrill
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val plan = remember(s.profile, s.blockStartEpochDay) { Store.plan(s) }
    val today = Store.todayEpochDay()
    val iso = Dates.isoDay(today)
    val todayPlan = plan.days.firstOrNull { it.isoDay == iso }
    val logDays = remember(s.log) { s.log.filter { it.completed }.map { it.epochDay }.toSet() }
    val doneToday = today in logDays
    val planned = plan.days.map { it.isoDay }.toSet()
    val streak = Streaks.planStreak(logDays, planned, today)
    val weeks = Streaks.weekStreak(logDays, planned.size, today)
    val weekMin = s.log.filter { Dates.mondayOf(it.epochDay) == Dates.mondayOf(today) }.sumOf { it.durationSec } / 60
    val next = remember(s.reminders, s.snoozes, LocalResume.current) { AlarmScheduler.nextAlarm() }
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))
            Header(greeting(), date.replaceFirstChar { it.uppercase() }) {
                IconButton({ nav.push(Route.Settings) }) { Icon(Icons.Default.Settings, "Настройки") }
            }
        }
        item {
            val off by animateFloatAsState(if (appear) 0f else 40f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "hero")
            Box(Modifier.padding(horizontal = 16.dp).graphicsLayer { translationY = off; alpha = 1f - off / 40f }) {
                HeroCard(todayPlan?.session?.title, todayPlan?.session?.estimatedMin ?: 0, doneToday, plan.deload,
                    nextTitle = plan.days.firstOrNull { it.isoDay > iso }?.let { "${ReminderMath.DAY_FULL[it.isoDay - 1]}: ${it.session.title}" }
                        ?: plan.days.firstOrNull()?.let { "${ReminderMath.DAY_FULL[it.isoDay - 1]}: ${it.session.title}" },
                    onStart = { todayPlan?.let { nav.openPlayer(it.session) } },
                    onRestActivity = { nav.openPlayer(Actions.quickSession("Мобильность и навыки, 10 мин", listOf(PlannedDrill("st_stance", 1, 60, 15), PlannedDrill("st_footwork", 2, 90, 30), PlannedDrill("sk_guard", 2, 60, 30), PlannedDrill("aw_voice", 1, 60, 15)))) })
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$streak", "серия по плану", AppIcons.Flame, Ember, Modifier.weight(1f))
                StatTile("$weeks", "недель подряд", AppIcons.Chart, Mint, Modifier.weight(1f))
                StatTile("$weekMin", "минут за неделю", AppIcons.Timer, Sky, Modifier.weight(1f))
            }
        }
        item { WeekStrip(planned, logDays, today) }
        item {
            BCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), onClick = { nav.tab = 3 }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(AppIcons.Bell, Amber)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Следующее напоминание", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (next == null) Text("Нет активных", style = MaterialTheme.typography.titleMedium)
                        else {
                            val r = s.reminders.firstOrNull { it.id == next.reminderId }
                            val t = Instant.ofEpochMilli(next.atMillis).atZone(ZoneId.systemDefault())
                            val day = when (t.toLocalDate().toEpochDay() - today) { 0L -> "сегодня"; 1L -> "завтра"; else -> ReminderMath.DAY_SHORT[t.dayOfWeek.value - 1] }
                            Text("$day %02d:%02d · ${r?.title ?: ""}".format(t.hour, t.minute), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            if (next.mode == org.ivanivorontsov.boec.model.AlertMode.CALL) Text("придёт как входящий звонок", style = MaterialTheme.typography.bodySmall, color = Ember)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Быстро", Modifier.padding(horizontal = 20.dp)) }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("5 минут навыков", "стойка · перемещения · бой с тенью", AppIcons.Shield, Mint, Modifier.weight(1f)) {
                    nav.openPlayer(Actions.quickSession("5 минут навыков", listOf(PlannedDrill("st_stance", 1, 45, 15), PlannedDrill("st_footwork", 1, 60, 20), PlannedDrill("cd_rounds", 1, 90, 0)), warm = 1))
                }
                QuickTile("Встреча с собакой", "что делать прямо сейчас", AppIcons.Paw, Amber, Modifier.weight(1f)) { nav.push(Route.AnimalDetail("dogs")) }
            }
        }
        item {
            BCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), color = Danger.copy(alpha = 0.12f),
                onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Danger), contentAlignment = Alignment.Center) { Icon(Icons.Default.Phone, null, tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Экстренный вызов 112", style = MaterialTheme.typography.titleMedium, color = Danger)
                        Text("Откроет набор номера. Звонок бесплатный.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            val tip = remember(today) { tipOfDay(today) }
            BCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("СОВЕТ ДНЯ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(tip.first, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(tip.second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when (h) { in 5..11 -> "Доброе утро"; in 12..17 -> "Добрый день"; in 18..22 -> "Добрый вечер"; else -> "Доброй ночи" }
}

private fun tipOfDay(day: Long): Pair<String, String> {
    val tips = Science.principles.map { it.title to it.body.first() } +
        Skills.principles.map { it.title to it.body.first() } +
        Skills.drills.map { it.name to it.why }
    return tips[Math.floorMod(day, tips.size.toLong()).toInt()]
}

@Composable
private fun HeroCard(title: String?, minutes: Int, done: Boolean, deload: Boolean, nextTitle: String?, onStart: () -> Unit, onRestActivity: () -> Unit) {
    val brush = when {
        done -> Brush.linearGradient(listOf(Color(0xFF1FAE73), Color(0xFF0E6E4B)))
        title != null -> Brush.linearGradient(listOf(Color(0xFFFF7A45), Color(0xFFD6402A), Color(0xFF8E1F1A)))
        else -> Brush.linearGradient(listOf(Color(0xFF2B3A55), Color(0xFF1A2236)))
    }
    BCard(Modifier.fillMaxWidth(), brush = brush, padding = 22.dp) {
        val white = Color.White
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (done) "СДЕЛАНО" else if (title != null) "ТРЕНИРОВКА СЕГОДНЯ" else "ДЕНЬ ВОССТАНОВЛЕНИЯ", color = white.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (deload && title != null) Text("разгрузка", Modifier.clip(RoundedCornerShape(50)).background(white.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 3.dp), color = white, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when { done -> "Отличная работа! Восстановление — тоже часть тренировки."; title != null -> title; else -> "Прогулка 30 мин или лёгкая мобильность" },
            color = white, style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when { title != null && !done -> "≈ $minutes мин · разминка, основная часть, заминка"; else -> nextTitle?.let { "Дальше — $it" } ?: "" },
            color = white.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = if (title != null && !done) onStart else onRestActivity,
            colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = Color(0xFF1A1C20)),
            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Icon(if (done) Icons.Default.Check else Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (title != null && !done) "Начать тренировку" else "10 минут мобильности", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeekStrip(planned: Set<Int>, logDays: Set<Long>, today: Long) {
    val monday = Dates.mondayOf(today)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val d = monday + i
            val isPlanned = (i + 1) in planned
            val done = d in logDays
            val isToday = d == today
            val bg = when { done -> Mint; isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f); else -> Color.Transparent }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ReminderMath.DAY_SHORT[i], style = MaterialTheme.typography.labelSmall, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(bg)
                        .border(2.dp, if (isPlanned && !done) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    else Text("${LocalDate.ofEpochDay(d).dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = if (isPlanned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    BCard(modifier, onClick = onClick, padding = 14.dp) {
        IconBadge(icon, tint, 38.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}
