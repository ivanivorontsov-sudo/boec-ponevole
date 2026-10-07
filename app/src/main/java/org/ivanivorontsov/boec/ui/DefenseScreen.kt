package org.ivanivorontsov.boec.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ivanivorontsov.boec.content.AnimalGuide
import org.ivanivorontsov.boec.content.Animals
import org.ivanivorontsov.boec.content.Skills
import org.ivanivorontsov.boec.model.PlannedDrill
import java.time.LocalDate

@Composable
fun DefenseScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Header("Защита", "Самооборона и опасные животные")
        SegmentedTabs(listOf("Самооборона", "Животные"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "def") { t ->
            if (t == 0) SelfDefenseTab() else AnimalsTab()
        }
    }
}

@Composable
private fun SelfDefenseTab() {
    val nav = LocalNav.current
    val cats = Skills.drills.groupBy { it.category }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            BCard(Modifier.fillMaxWidth(), brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF15805A), Color(0xFF0B4F39)))) {
                Text("Главное правило", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text("Избегай → деэскалируй → уходи → защищайся, только если иначе нельзя", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ nav.openPlayer(org.ivanivorontsov.boec.domain.ScheduleGenerator.buildSkillsSession(org.ivanivorontsov.boec.data.Store.value.profile.copy(minutes = 25), 0, false)) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0B4F39)), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Text("Удары и стойка")
                    }
                    Button({ nav.openPlayer(org.ivanivorontsov.boec.domain.ScheduleGenerator.buildSkillsSession(org.ivanivorontsov.boec.data.Store.value.profile.copy(minutes = 25), 1, false)) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Text("Падения и уход")
                    }
                }
            }
        }
        items(Skills.principles) { InfoCardView(it, accent = Mint) }
        item { InfoCardView(Skills.law, accent = Sky) }
        cats.forEach { (cat, drills) ->
            item { SectionTitle(cat) }
            items(drills, key = { it.id }) { d ->
                BCard(Modifier.fillMaxWidth(), onClick = { nav.push(Route.DrillDetail(d.id)) }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.titleMedium)
                            Text(d.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        if (d.needsMat) Pill("мат", Amber)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalsTab() {
    val nav = LocalNav.current
    val month = LocalDate.now().monthValue
    LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(2) }) { EmergencyCard() }
        items(Animals.all, key = { it.id }) { a ->
            BCard(Modifier.fillMaxWidth().height(150.dp), onClick = { nav.push(Route.AnimalDetail(a.id)) }, padding = 14.dp) {
                Text(a.emoji, fontSize = 34.sp)
                Spacer(Modifier.height(8.dp))
                Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 3, modifier = Modifier.weight(1f))
                if (month in a.season) Pill("сезон", Amber)
            }
        }
    }
}

@Composable
fun EmergencyCard() {
    val ctx = LocalContext.current
    BCard(Modifier.fillMaxWidth(), color = Danger.copy(alpha = 0.12f), onClick = {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Danger), contentAlignment = Alignment.Center) { Icon(Icons.Default.Phone, null, tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column { Text("Укус, нападение, анафилаксия — 112", style = MaterialTheme.typography.titleMedium, color = Danger); Text("Нажми, чтобы набрать", style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(8.dp))
        Bullets(Animals.emergency, Danger)
    }
}

@Composable
fun AnimalDetailScreen(id: String) {
    val nav = LocalNav.current
    val a: AnimalGuide = Animals.get(id)
    Column(Modifier.fillMaxSize()) {
        Header(a.name, onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.emoji, fontSize = 56.sp)
                Spacer(Modifier.width(14.dp))
                Text(a.summary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            Section("Как не встретиться", a.prevention, Mint)
            Section("При встрече", a.encounter, Sky)
            Section("Если напал", a.attack, Ember)
            Section("Первая помощь", a.firstAid, Amber)
            if (a.dont.isNotEmpty()) BCard(Modifier.fillMaxWidth(), color = Danger.copy(alpha = 0.12f)) {
                Text("НЕЛЬЗЯ", style = MaterialTheme.typography.titleMedium, color = Danger); Spacer(Modifier.height(8.dp)); Bullets(a.dont, Danger)
            }
            EmergencyCard()
            Text("Источники: " + a.sources.joinToString("; "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Справка не заменяет обучение первой помощи и вызов скорой.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

@Composable
private fun Section(title: String, items: List<String>, color: Color) {
    if (items.isEmpty()) return
    BCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        Spacer(Modifier.height(8.dp))
        Bullets(items, color)
    }
}

@Composable
fun DrillDetailScreen(id: String) {
    val nav = LocalNav.current
    val d = Skills.get(id)
    Column(Modifier.fillMaxSize()) {
        Header(d.name, d.category, onBack = { nav.pop() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BCard(Modifier.fillMaxWidth(), color = Mint.copy(alpha = 0.12f)) { Text("Зачем", style = MaterialTheme.typography.titleMedium, color = Mint); Spacer(Modifier.height(6.dp)); Text(d.why) }
            Section("Как выполнять", d.how, Sky)
            Section("Безопасность", d.safety + listOfNotNull(if (d.needsMat) "Нужен мягкий пол: мат, газон или матрас." else null), Amber)
            BCard(Modifier.fillMaxWidth()) {
                Text("С партнёром — только с тренером", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Работа в паре, броски и болевые приёмы требуют контроля квалифицированного инструктора.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { nav.openPlayer(Actions.quickSession(d.name, listOf(if (d.needsMat || d.id in setOf("rl_wrist", "gr_standup", "aw_exit")) PlannedDrill(d.id, 3, 0, 30, reps = 8) else PlannedDrill(d.id, 3, 60, 30)), warm = 2)) },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Отработать: 3 подхода") }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}
