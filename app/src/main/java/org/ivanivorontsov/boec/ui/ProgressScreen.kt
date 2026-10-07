package org.ivanivorontsov.boec.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.Streaks
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen() {
    val s by Store.state.collectAsState()
    val today = Store.todayEpochDay()
    val plan = remember(s.profile, s.blockStartEpochDay) { Store.plan(s) }
    val logDays = remember(s.log) { s.log.filter { it.completed }.map { it.epochDay }.toSet() }
    val planned = plan.days.map { it.isoDay }.toSet()
    val weeks = remember(s.log) { Streaks.minutesPerWeek(s.log, today, 8) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Header("Прогресс", "Серии, минуты и история") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${Streaks.planStreak(logDays, planned, today)}", "серия по плану", AppIcons.Flame, Ember, Modifier.weight(1f))
                StatTile("${Streaks.weekStreak(logDays, planned.size, today)}", "недель с планом", AppIcons.Chart, Mint, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${s.log.count { it.completed }}", "тренировок всего", AppIcons.Dumbbell, Sky, Modifier.weight(1f))
                StatTile("${s.log.sumOf { it.durationSec } / 60}", "минут всего", AppIcons.Timer, Amber, Modifier.weight(1f))
            }
        }
        item { WeeklyChart(weeks) }
        item { Heatmap(logDays, today) }
        item { SectionTitle("История", Modifier.padding(horizontal = 20.dp)) }
        if (s.log.isEmpty()) item {
            Text("Пока пусто. Первая тренировка — самый большой шаг: польза от «ничего → что-то» самая заметная (ACSM 2026).",
                Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(s.log.sortedByDescending { it.startedAt }.take(60)) { e ->
            val dt = Instant.ofEpochMilli(e.startedAt).atZone(ZoneId.systemDefault())
            BCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(dt.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))) + " · ${e.durationSec / 60} мин · ${e.sets} подх." + if (e.avgRpe > 0) " · RPE %.1f".format(e.avgRpe) else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when {
                        e.stoppedForPain -> Pill("стоп: боль", Danger)
                        !e.completed -> Pill("частично", Amber)
                        else -> Pill("готово", Mint)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyChart(minutes: List<Int>) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(minutes) { anim.snapTo(0f); anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val who = Mint
    BCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Минут тренировок по неделям", style = MaterialTheme.typography.titleMedium)
        Text("Пунктир — 150 мин (минимум ВОЗ для аэробной активности; прогулки тоже считаются)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        val max = (minutes.maxOrNull() ?: 0).coerceAtLeast(180).toFloat()
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = minutes.size
            val gap = 10.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            val y150 = size.height * (1 - 150f / max)
            drawLine(who, Offset(0f, y150), Offset(size.width, y150), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            minutes.forEachIndexed { i, m ->
                val h = size.height * (m / max) * anim.value
                drawRoundRect(
                    color = if (i == n - 1) primary else primary.copy(alpha = 0.55f),
                    topLeft = Offset(i * (bw + gap), size.height - h), size = Size(bw, h), cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("8 нед. назад", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("эта неделя: ${minutes.lastOrNull() ?: 0} мин", style = MaterialTheme.typography.labelSmall, color = primary)
        }
    }
}

@Composable
private fun Heatmap(days: Set<Long>, today: Long) {
    val weeks = 12
    val start = Dates.mondayOf(today) - 7L * (weeks - 1)
    BCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Календарь за 12 недель", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (w in 0 until weeks) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (d in 0..6) {
                        val day = start + w * 7 + d
                        val c = when {
                            day > today -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            day in days -> Mint
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(c))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(LocalDate.ofEpochDay(start).format(DateTimeFormatter.ofPattern("d MMM", Locale("ru"))) + " — сегодня", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

