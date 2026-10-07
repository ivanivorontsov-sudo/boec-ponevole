package org.ivanivorontsov.boec.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.ivanivorontsov.boec.alarm.AlarmScheduler
import org.ivanivorontsov.boec.alarm.Notifier
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.ReminderMath
import org.ivanivorontsov.boec.model.AlertMode
import org.ivanivorontsov.boec.model.Reminder
import org.ivanivorontsov.boec.model.ReminderKind
import org.ivanivorontsov.boec.model.Snooze
import org.ivanivorontsov.boec.util.Battery

fun kindIcon(k: ReminderKind): Pair<ImageVector, Color> = when (k) {
    ReminderKind.WORKOUT -> AppIcons.Dumbbell to Ember
    ReminderKind.SKILL -> AppIcons.Shield to Mint
    ReminderKind.WATER -> AppIcons.Water to Sky
    ReminderKind.POSTURE -> AppIcons.Timer to Amber
    ReminderKind.SLEEP -> AppIcons.Moon to Color(0xFF9C8CFF)
    ReminderKind.STRETCH -> AppIcons.Fist to Mint
    ReminderKind.TICKS -> AppIcons.Paw to Amber
    ReminderKind.FIRST_AID -> AppIcons.Medkit to Danger
    ReminderKind.EYES -> AppIcons.Timer to Sky
    ReminderKind.CUSTOM -> AppIcons.Bell to Ember
}

object TestCall {
    fun schedule(ctx: android.content.Context, delaySec: Int) {
        val r = Reminder(Notifier.TEST_ID, ReminderKind.CUSTOM, "Проверка звонка", "Так будет выглядеть звонок о тренировке. Ответь или отклони.",
            enabled = false, mode = AlertMode.CALL, daysMask = 0, hour = 0, minute = 0)
        Store.update { s ->
            s.copy(reminders = s.reminders.filter { it.id != r.id } + r,
                snoozes = s.snoozes.filter { it.reminderId != r.id } + Snooze(r.id, System.currentTimeMillis() + delaySec * 1000L, 5))
        }
        AlarmScheduler.rescheduleAll(ctx)
        Toast.makeText(ctx, "Звонок через $delaySec с — можно заблокировать экран", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun RemindersScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val tick = LocalResume.current
    val problems = remember(tick) {
        listOfNotNull(
            if (!Notifier.canPost(ctx)) "уведомления" else null,
            if (!AlarmScheduler.canExact(ctx)) "точное время" else null,
            if (!Notifier.canFullScreen(ctx)) "полноэкранный звонок" else null,
            if (!Battery.isIgnoring(ctx)) "батарея" else null,
        )
    }
    val visible = s.reminders.filter { it.id != Notifier.TEST_ID }
    val groups = listOf(
        "Тренировки по плану" to visible.filter { it.kind == ReminderKind.WORKOUT || it.kind == ReminderKind.SKILL }.sortedBy { it.planDay },
        "Полезные" to visible.filter { it.kind != ReminderKind.WORKOUT && it.kind != ReminderKind.SKILL && it.kind != ReminderKind.CUSTOM },
        "Свои" to visible.filter { it.kind == ReminderKind.CUSTOM },
    )
    Column(Modifier.fillMaxSize()) {
        Header("Напоминания", "Звонки и уведомления") {
            IconButton({ nav.push(Route.EditReminder(null)) }) { Icon(Icons.Default.Add, "Добавить") }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (problems.isNotEmpty()) item {
                BCard(Modifier.fillMaxWidth(), color = Amber.copy(alpha = 0.14f), onClick = { nav.push(Route.Settings) }) {
                    Text("Напоминания могут не прийти", style = MaterialTheme.typography.titleMedium, color = Amber)
                    Text("Не настроено: ${problems.joinToString()}. Нажми, чтобы исправить.", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Call, Mint)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Проверить звонок", style = MaterialTheme.typography.titleMedium)
                            Text("Заблокируй экран после нажатия", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton({ TestCall.schedule(ctx, 10) }, shape = RoundedCornerShape(12.dp)) { Text("Через 10 с") }
                    }
                }
            }
            groups.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    item { SectionTitle(title) }
                    items(list, key = { it.id }) { r -> ReminderRow(r, Modifier.animateItem()) { nav.push(Route.EditReminder(r.id)) } }
                }
            }
            item {
                OutlinedButton({ nav.push(Route.EditReminder(null)) }, Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Add, null); Text("Своё напоминание")
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(r: Reminder, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val (icon, tint) = kindIcon(r.kind)
    BCard(modifier.fillMaxWidth(), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(ReminderMath.describe(r), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Pill(if (r.mode == AlertMode.CALL) "☎ звонок" else "уведомление", if (r.mode == AlertMode.CALL) Ember else Sky)
            }
            Switch(
                checked = r.enabled,
                onCheckedChange = { v -> Store.update { s -> s.copy(reminders = s.reminders.map { if (it.id == r.id) it.copy(enabled = v) else it }) }; AlarmScheduler.rescheduleAll(ctx) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun EditReminderScreen(id: String?) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val existing = remember(id) { Store.value.reminders.firstOrNull { it.id == id } }
    val isPlan = existing?.kind == ReminderKind.WORKOUT || existing?.kind == ReminderKind.SKILL
    var r by remember {
        mutableStateOf(existing ?: Reminder("custom_" + System.currentTimeMillis(), ReminderKind.CUSTOM, "", "", hour = 12, minute = 0))
    }
    var showTime by remember { mutableStateOf(false) }
    var showUntil by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Header(if (existing == null) "Новое напоминание" else "Напоминание", onBack = { nav.pop() }) {
            if (existing?.kind == ReminderKind.CUSTOM) IconButton({
                Store.update { s -> s.copy(reminders = s.reminders.filter { it.id != r.id }, snoozes = s.snoozes.filter { it.reminderId != r.id }) }
                AlarmScheduler.rescheduleAll(ctx); nav.pop()
            }) { Icon(Icons.Default.Delete, "Удалить") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (isPlan) {
                Text(r.title, style = MaterialTheme.typography.titleLarge)
                Text("Время и дни задаются в параметрах плана.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({ nav.push(Route.EditProfile) }) { Text("Открыть параметры плана") }
            } else {
                OutlinedTextField(r.title, { r = r.copy(title = it) }, Modifier.fillMaxWidth(), label = { Text("Название") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(r.text, { r = r.copy(text = it) }, Modifier.fillMaxWidth(), label = { Text("Текст") }, minLines = 2)
            }
            SectionTitle("Как напоминать")
            SegmentedTabs(AlertMode.entries.map { it.title }, r.mode.ordinal, { r = r.copy(mode = AlertMode.entries[it]) })
            Text(if (r.mode == AlertMode.CALL) "Полноэкранный «входящий звонок» с мелодией и вибрацией. «Отклонить» = отложить на ${Store.value.settings.snoozeMin} мин." else "Обычное уведомление с кнопками «Готово» и «Отложить».",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            if (!isPlan) {
                SectionTitle("Время")
                BCard(Modifier.fillMaxWidth(), onClick = { showTime = true }, padding = 14.dp) {
                    Text("%02d:%02d".format(r.hour, r.minute), style = MaterialTheme.typography.headlineSmall.merge(MonoDigits))
                }
                SectionTitle("Повтор в течение дня")
                ChoiceChips(listOf(0, 30, 60, 90, 120, 180), { r.repeatEveryMin == it }, { if (it == 0) "Один раз" else "каждые $it мин" }) {
                    r = r.copy(repeatEveryMin = it, untilHour = if (it > 0 && r.untilHour == 0) 21 else r.untilHour)
                }
                if (r.repeatEveryMin > 0) TextButton({ showUntil = true }) { Text("до %02d:%02d".format(r.untilHour, r.untilMinute)) }
                SectionTitle("Дни недели")
                ChoiceChips((1..7).toList(), { r.dayOfMonth == 0 && (r.daysMask and (1 shl (it - 1))) != 0 }, { ReminderMath.DAY_SHORT[it - 1] }) { d ->
                    r = r.copy(dayOfMonth = 0, daysMask = r.daysMask xor (1 shl (d - 1)))
                }
                SectionTitle("Месяцы (сезон)")
                ChoiceChips((1..12).toList(), { (r.monthsMask and (1 shl (it - 1))) != 0 }, { ReminderMath.MONTH_SHORT[it - 1] }) { m ->
                    r = r.copy(monthsMask = r.monthsMask xor (1 shl (m - 1)))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                val fixed = if (r.title.isBlank()) r.copy(title = "Напоминание") else r
                Store.update { s -> s.copy(reminders = s.reminders.filter { it.id != fixed.id } + fixed) }
                AlarmScheduler.rescheduleAll(ctx); nav.pop()
            },
            enabled = isPlan || r.daysMask != 0 || r.dayOfMonth > 0,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp), shape = RoundedCornerShape(16.dp),
        ) { Text("Сохранить") }
    }
    if (showTime || showUntil) {
        val st = rememberTimePickerState(if (showTime) r.hour else r.untilHour, if (showTime) r.minute else r.untilMinute, true)
        AlertDialog(
            onDismissRequest = { showTime = false; showUntil = false },
            confirmButton = { TextButton({
                r = if (showTime) r.copy(hour = st.hour, minute = st.minute) else r.copy(untilHour = st.hour, untilMinute = st.minute)
                showTime = false; showUntil = false
            }) { Text("Готово") } },
            dismissButton = { TextButton({ showTime = false; showUntil = false }) { Text("Отмена") } },
            text = { TimePicker(st) },
        )
    }
}
