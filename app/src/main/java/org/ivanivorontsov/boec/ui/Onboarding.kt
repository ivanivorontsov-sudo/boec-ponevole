package org.ivanivorontsov.boec.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import org.ivanivorontsov.boec.alarm.Notifier
import org.ivanivorontsov.boec.content.Science
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.ReminderMath
import org.ivanivorontsov.boec.model.Equipment
import org.ivanivorontsov.boec.model.Goal
import org.ivanivorontsov.boec.model.Level
import org.ivanivorontsov.boec.model.Muscle
import org.ivanivorontsov.boec.model.Profile
import org.ivanivorontsov.boec.util.Battery

@Composable
fun OnboardingScreen() {
    val ctx = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var parq by remember { mutableStateOf(List<Boolean?>(Science.parq.size) { null }) }
    var profile by remember { mutableStateOf(Store.value.profile) }
    val steps = 5
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (step > 0) {
            val p by androidx.compose.animation.core.animateFloatAsState(step / (steps - 1f), label = "p")
            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clip(RoundedCornerShape(50)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), gapSize = 0.dp, drawStopIndicator = {})
        }
        Box(Modifier.weight(1f)) {
            AnimatedContent(step, transitionSpec = {
                val d = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(350)) { d * it / 4 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(300)) { -d * it / 4 } + fadeOut(tween(200)))
            }, label = "step") { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    when (s) {
                        0 -> Welcome()
                        1 -> Disclaimer(accepted) { accepted = it }
                        2 -> Parq(parq) { i, v -> parq = parq.toMutableList().also { l -> l[i] = v } }
                        3 -> { Text("Твой план", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(4.dp))
                            Text("План построится сам по твоим дням и времени. Изменить можно в любой момент.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ProfileForm(profile.copy(cautious = parq.any { it == true })) { profile = it } }
                        else -> { Text("Чтобы напоминания работали", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(4.dp))
                            Text("Звонки должны прийти вовремя даже при выключенном экране. Дай разрешения ниже — особенно на realme/OPPO.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp)); PermissionsPanel() }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, Modifier.height(54.dp)) { Text("Назад") }
            val enabled = when (step) { 1 -> accepted; 2 -> parq.all { it != null }; 3 -> profile.days.isNotEmpty(); else -> true }
            Button(
                onClick = {
                    if (step < steps - 1) step++ else {
                        val flags = parq.map { it == true }
                        Actions.finishOnboarding(ctx, profile.copy(cautious = flags.any { it }, level = if (flags.any { it }) Level.BEGINNER else profile.level), flags)
                    }
                },
                enabled = enabled, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp),
            ) { Text(when (step) { 0 -> "Начать"; steps - 1 -> "Готово — построить план"; else -> "Далее" }, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun Welcome() {
    val inf = rememberInfiniteTransition(label = "w")
    val pulse by inf.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Spacer(Modifier.height(32.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(150.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 0.35f }.clip(CircleShape).background(Brush.radialGradient(listOf(Ember, Color.Transparent))))
        Box(Modifier.size(104.dp).clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(Color(0xFFFF7A45), EmberDeep))), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Shield, null, tint = Color.White, modifier = Modifier.size(58.dp))
            Icon(AppIcons.Fist, null, tint = EmberDeep, modifier = Modifier.size(30.dp))
        }
    }
    Spacer(Modifier.height(28.dp))
    Text("Боец поневоле", style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(8.dp))
    Text("Научно обоснованные тренировки, навыки самообороны, защита от животных и полезные напоминания — со звонком, от которого не отвертишься.",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    listOf(
        Triple(AppIcons.Dumbbell, "Сила по науке", "План под твои дни и минуты: ВОЗ, ACSM 2026, мета-анализы Шонфельда"),
        Triple(AppIcons.Shield, "Самооборона", "Избегание и деэскалация, стойка, удары, освобождения, страховки"),
        Triple(AppIcons.Paw, "Животные", "Собаки, медведи, тигр, змеи, клещи, осы — что делать и первая помощь"),
        Triple(AppIcons.Bell, "Звонки-напоминания", "Полноэкранный «входящий» о тренировке + вода, сон, клещи"),
    ).forEachIndexed { i, (ic, t, d) ->
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(ic, listOf(Ember, Mint, Amber, Sky)[i])
            Spacer(Modifier.width(14.dp))
            Column { Text(t, style = MaterialTheme.typography.titleMedium); Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Без интернета, рекламы и трекеров. Все данные остаются на телефоне.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Disclaimer(accepted: Boolean, onAccept: (Boolean) -> Unit) {
    Text("Важно прочитать", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    BCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconBadge(AppIcons.Medkit, Danger); Spacer(Modifier.width(12.dp)); Text("Медицинское предупреждение", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(12.dp))
        Bullets(Science.disclaimer, Danger)
    }
    Spacer(Modifier.height(12.dp))
    BCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainerSafe()) {
        Text("Стоп, если больно", style = MaterialTheme.typography.titleMedium, color = Danger)
        Spacer(Modifier.height(6.dp))
        Text("Острая боль, боль в груди, головокружение, сильная одышка — немедленно прекрати. В плеере всегда есть красная кнопка «Боль — стоп».")
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onAccept(!accepted) }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(accepted, onAccept)
        Text("Я прочитал(а) и понимаю: приложение не заменяет врача и тренера")
    }
}

@Composable
fun androidx.compose.material3.ColorScheme.errorContainerSafe(): Color = Danger.copy(alpha = if (LocalDark.current) 0.12f else 0.08f)

@Composable
private fun Parq(answers: List<Boolean?>, onAnswer: (Int, Boolean) -> Unit) {
    Text("Готовность к нагрузке", style = MaterialTheme.typography.headlineMedium)
    Text("Общие вопросы анкеты PAR-Q+ (адаптированный перевод)", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    Science.parq.forEachIndexed { i, q ->
        BCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), padding = 14.dp) {
            Text("${i + 1}. $q", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                YesNo("Нет", answers[i] == false, Mint) { onAnswer(i, false) }
                YesNo("Да", answers[i] == true, Danger) { onAnswer(i, true) }
            }
        }
    }
    if (answers.any { it == true }) {
        Spacer(Modifier.height(8.dp))
        BCard(Modifier.fillMaxWidth(), color = Amber.copy(alpha = 0.14f)) { Bullets(Science.parqYes, Amber) }
    }
}

@Composable
private fun YesNo(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (selected) color else Color.Transparent, label = "yn")
    Box(
        Modifier.width(96.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(1.dp, if (selected) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun ProfileForm(p: Profile, onChange: (Profile) -> Unit) {
    var showTime by remember { mutableStateOf(false) }
    SectionTitle("Цель")
    Goal.entries.forEach { g ->
        val sel = p.goal == g
        val border by androidx.compose.animation.animateColorAsState(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, label = "g")
        BCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(2.dp, border, CardShape), onClick = { onChange(p.copy(goal = g)) }, padding = 14.dp) {
            Text(g.title, style = MaterialTheme.typography.titleMedium)
            Text(g.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    SectionTitle("Уровень")
    if (p.cautious) Text("Осторожный режим PAR-Q+: уровень «новичок».", color = Amber, style = MaterialTheme.typography.bodySmall)
    ChoiceChips(Level.entries, { p.level == it }, { it.title }) { if (!p.cautious) onChange(p.copy(level = it)) }
    SectionTitle("Дни тренировок")
    ChoiceChips((1..7).toList(), { it in p.days }, { ReminderMath.DAY_SHORT[it - 1] }) { d ->
        onChange(p.copy(days = if (d in p.days) p.days - d else (p.days + d).sorted()))
    }
    Text(
        when (p.days.size) { 0 -> "Выбери хотя бы один день"; 1 -> "1 день — минимум. Лучше 2–3."; in 2..4 -> "Отлично: каждая мышца будет нагружена 2+ раза в неделю."; else -> "Много дней: часть будет кардио/навыки для восстановления." },
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionTitle("Время звонка-напоминания")
    BCard(Modifier.fillMaxWidth(), onClick = { showTime = true }, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Timer, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("%02d:%02d".format(p.hour, p.minute), style = MaterialTheme.typography.headlineSmall.merge(MonoDigits), modifier = Modifier.weight(1f))
            Text("Изменить", color = MaterialTheme.colorScheme.primary)
        }
    }
    SectionTitle("Длительность: ${p.minutes} мин")
    Slider(value = p.minutes.toFloat(), onValueChange = { onChange(p.copy(minutes = (it / 5).toInt() * 5)) }, valueRange = 15f..90f, steps = 14)
    SectionTitle("Инвентарь")
    ChoiceChips(Equipment.entries, { it == Equipment.NONE || it in p.equipment }, { it.title }) { e ->
        if (e != Equipment.NONE) onChange(p.copy(equipment = if (e in p.equipment) p.equipment - e else p.equipment + e))
    }
    SectionTitle("Приоритетные мышцы (необязательно)")
    ChoiceChips(Muscle.entries, { it in p.focus }, { it.title }) { m ->
        onChange(p.copy(focus = if (m in p.focus) p.focus - m else (p.focus + m).take(3)))
    }
    Text("До 3 групп: получат дополнительное упражнение и подход.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    if (showTime) {
        val st = rememberTimePickerState(p.hour, p.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton({ onChange(p.copy(hour = st.hour, minute = st.minute)); showTime = false }) { Text("Готово") } },
            dismissButton = { TextButton({ showTime = false }) { Text("Отмена") } },
            text = { TimePicker(st) },
        )
    }
}

data class PermRow(val title: String, val desc: String, val ok: Boolean, val action: String, val onFix: () -> Unit)

@Composable
fun PermissionsPanel() {
    val ctx = LocalContext.current
    val tick = LocalResume.current
    var refresh by remember { mutableIntStateOf(0) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val rows = key(tick, refresh) {
        buildList {
            add(PermRow("Уведомления", "Без них не будет ни напоминаний, ни звонков.", Notifier.canPost(ctx), "Разрешить") {
                if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(ctx)
            })
            add(PermRow("Точное время", "Будильники срабатывают минута в минуту.", AlarmScheduler.canExact(ctx), "Разрешить") {
                if (Build.VERSION.SDK_INT >= 31) runCatching {
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            })
            add(PermRow("Полноэкранный звонок", "Экран «входящего вызова» поверх блокировки (Android 14+).", Notifier.canFullScreen(ctx), "Включить") {
                if (Build.VERSION.SDK_INT >= 34) {
                    val ok = runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.isSuccess
                    if (!ok) openNotificationSettings(ctx)
                }
            })
            add(PermRow("Без ограничений батареи", "Иначе система может «усыпить» напоминания.", Battery.isIgnoring(ctx), "Исключить") { Battery.requestIgnore(ctx) })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { r ->
            BCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(if (r.ok) Mint else Amber))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium)
                        Text(r.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (r.ok) Text("Готово", color = Mint, style = MaterialTheme.typography.labelLarge)
                    else Button(onClick = r.onFix, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(r.action) }
                }
            }
        }
        val (brand, steps) = Battery.oemSteps()
        InfoCardViewWithAction(
            title = "Настройки для $brand", body = steps, actionText = "Открыть настройки",
            open = Battery.isOppoFamily, onAction = { Battery.openOemSettings(ctx) },
        )
    }
}

@Composable
fun InfoCardViewWithAction(title: String, body: List<String>, actionText: String, open: Boolean, onAction: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(open) }
    BCard(Modifier.fillMaxWidth().animateContentSize(), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(AppIcons.Bell, Sky, 36.dp); Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Bullets(body.mapIndexed { i, s -> "${i + 1}. $s" }, Sky)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(actionText) }
        }
    }
}

fun openNotificationSettings(ctx: Context) {
    runCatching {
        ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
