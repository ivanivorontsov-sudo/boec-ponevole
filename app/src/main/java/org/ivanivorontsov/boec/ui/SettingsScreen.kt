package org.ivanivorontsov.boec.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.ivanivorontsov.boec.BuildConfig
import org.ivanivorontsov.boec.alarm.Notifier
import org.ivanivorontsov.boec.content.Animals
import org.ivanivorontsov.boec.content.Science
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.model.Reminder
import org.ivanivorontsov.boec.model.ReminderKind
import org.ivanivorontsov.boec.model.Settings
import org.ivanivorontsov.boec.model.ThemeMode
import org.ivanivorontsov.boec.util.Speaker

private fun setSettings(f: (Settings) -> Settings) { Store.update { it.copy(settings = f(it.settings)) } }

@Composable
fun SettingsScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    val speaker = remember { Speaker(ctx) { true } }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    Column(Modifier.fillMaxSize()) {
        Header("Настройки", onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Тема")
            SegmentedTabs(ThemeMode.entries.map { it.title }, s.settings.theme.ordinal, { i -> setSettings { it.copy(theme = ThemeMode.entries[i]) } })
            SectionTitle("Голосовые подсказки")
            BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Озвучивать тренировку", style = MaterialTheme.typography.titleMedium)
                        Text("Подход, отдых, обратный отсчёт — по-русски", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(s.settings.tts, { v -> setSettings { it.copy(tts = v) } })
                }
                OutlinedButton({ speaker.say("Подход один из трёх. Приседания. Восемь – двенадцать повторов.") }, shape = RoundedCornerShape(12.dp)) { Text("Проверить голос") }
                Text("Нет голоса? Установи русский голос синтеза речи в настройках телефона (Язык и ввод → Синтез речи).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionTitle("Звонок-напоминание")
            BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Text("Отложить на", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(listOf(5, 10, 15, 30), { s.settings.snoozeMin == it }, { "$it мин" }) { v -> setSettings { it.copy(snoozeMin = v) } }
                Spacer(Modifier.height(8.dp))
                Text("Звонить не дольше", style = MaterialTheme.typography.titleSmall)
                ChoiceChips(listOf(30, 60, 90), { s.settings.callTimeoutSec == it }, { "$it с" }) { v -> setSettings { it.copy(callTimeoutSec = v) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Вибрация", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(s.settings.vibrate, { v -> setSettings { it.copy(vibrate = v) } })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({
                        val r = Reminder(Notifier.TEST_ID, ReminderKind.CUSTOM, "Проверка звонка", "Так выглядит звонок о тренировке.", enabled = false, hour = 0, minute = 0)
                        ctx.startActivity(Notifier.callIntent(ctx, r, preview = true))
                    }, shape = RoundedCornerShape(12.dp)) { Text("Показать сейчас") }
                    OutlinedButton({ TestCall.schedule(ctx, 10) }, shape = RoundedCornerShape(12.dp)) { Text("Через 10 с") }
                }
            }
            SectionTitle("Разрешения и батарея")
            PermissionsPanel()
            SectionTitle("Безопасность и наука")
            BCard(Modifier.fillMaxWidth(), onClick = { nav.push(Route.Safety) }, padding = 14.dp) {
                Text("Медицинское предупреждение и PAR-Q+", style = MaterialTheme.typography.titleMedium)
                Text(if (s.profile.cautious) "Сейчас включён осторожный режим" else "Пройти анкету заново", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            BCard(Modifier.fillMaxWidth(), onClick = { nav.push(Route.Sources) }, padding = 14.dp) {
                Text("Источники", style = MaterialTheme.typography.titleMedium)
                Text("${Science.sources.size} научных и официальных источников", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionTitle("О приложении")
            BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                KeyValue("Версия", BuildConfig.VERSION_NAME)
                KeyValue("Интернет", "не используется")
                KeyValue("Реклама и трекеры", "нет")
                KeyValue("Данные", "только на телефоне")
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
fun SourcesScreen() {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize()) {
        Header("Источники", onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Science.sources.forEach { src ->
                BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Text(src.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(src.detail, style = MaterialTheme.typography.bodySmall)
                }
            }
            SectionTitle("Животные: по разделам")
            Animals.all.forEach { a -> Text("${a.emoji} ${a.name}: " + a.sources.joinToString("; "), style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
fun SafetyScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val s by Store.state.collectAsState()
    var answers by remember { mutableStateOf(s.parq.ifEmpty { List(Science.parq.size) { false } }) }
    Column(Modifier.fillMaxSize()) {
        Header("Безопасность", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BCard(Modifier.fillMaxWidth()) { Text("Медицинское предупреждение", style = MaterialTheme.typography.titleMedium, color = Danger); Spacer(Modifier.height(8.dp)); Bullets(Science.disclaimer, Danger) }
            SectionTitle("PAR-Q+ (общие вопросы)")
            Science.parq.forEachIndexed { i, q ->
                BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(answers.getOrElse(i) { false }, { v -> answers = answers.toMutableList().also { it[i] = v } })
                    }
                }
            }
            Text("Включено = «да». Если врач разрешил нагрузку, можно снять отметки и выйти из осторожного режима.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = {
                val cautious = answers.any { it }
                Store.update { it.copy(parq = answers) }
                Actions.applyProfile(ctx, s.profile.copy(cautious = cautious, level = if (cautious) org.ivanivorontsov.boec.model.Level.BEGINNER else s.profile.level), restartBlock = false)
                nav.pop()
            },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp), shape = RoundedCornerShape(16.dp),
        ) { Text("Сохранить") }
    }
}
