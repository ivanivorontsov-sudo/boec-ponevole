package org.ivanivorontsov.boec.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.ivanivorontsov.boec.alarm.AlarmScheduler
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.ReminderFactory
import org.ivanivorontsov.boec.domain.ScheduleGenerator
import org.ivanivorontsov.boec.model.PlannedDrill
import org.ivanivorontsov.boec.model.Profile
import org.ivanivorontsov.boec.model.Session
import org.ivanivorontsov.boec.model.SessionType

sealed interface Route {
    data object Tabs : Route
    data class Player(val session: Session) : Route
    data class ExerciseDetail(val id: String) : Route
    data class DrillDetail(val id: String) : Route
    data class AnimalDetail(val id: String) : Route
    data object Settings : Route
    data object Sources : Route
    data object EditProfile : Route
    data object Safety : Route
    data class EditReminder(val id: String?) : Route
}

class Nav {
    val stack = mutableStateListOf<Route>(Route.Tabs)
    var tab by mutableIntStateOf(0)
    val top: Route get() = stack.last()
    fun push(r: Route) { stack.add(r) }
    fun pop(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun openPlayer(s: Session) { while (stack.size > 1 && stack.last() is Route.Player) pop(); push(Route.Player(s)) }
}

val LocalNav = compositionLocalOf { Nav() }
val LocalResume = compositionLocalOf { 0 }

object Actions {
    fun finishOnboarding(ctx: Context, profile: Profile, parq: List<Boolean>) {
        val today = Store.todayEpochDay()
        Store.update { s ->
            val plan = ScheduleGenerator.generate(profile, 0)
            val existing = s.reminders.ifEmpty { ReminderFactory.defaults() }
            s.copy(
                onboarded = true, disclaimerAccepted = true, parq = parq, profile = profile,
                blockStartEpochDay = Dates.mondayOf(today),
                reminders = existing.filter { !it.id.startsWith("workout_") } + ReminderFactory.forPlan(profile, plan, existing),
            )
        }
        AlarmScheduler.rescheduleAll(ctx)
    }

    fun applyProfile(ctx: Context, profile: Profile, restartBlock: Boolean) {
        Store.update { s ->
            val st = s.copy(profile = profile, blockStartEpochDay = if (restartBlock) Dates.mondayOf(Store.todayEpochDay()) else s.blockStartEpochDay)
            val plan = Store.plan(st)
            st.copy(reminders = s.reminders.filter { !it.id.startsWith("workout_") } + ReminderFactory.forPlan(profile, plan, s.reminders))
        }
        AlarmScheduler.rescheduleAll(ctx)
    }

    fun quickSession(title: String, drills: List<PlannedDrill>, warm: Int = 2): Session =
        Session(SessionType.SKILLS, title, warm, emptyList(), drills, 0, 1, warm + 1 + drills.sumOf { ScheduleGenerator.drillSeconds(it) } / 60)
}

private data class TabItem(val title: String, val icon: ImageVector)
private val tabs = listOf(
    TabItem("Сегодня", AppIcons.Home), TabItem("Тренировки", AppIcons.Dumbbell), TabItem("Защита", AppIcons.Shield),
    TabItem("Напоминания", AppIcons.Bell), TabItem("Прогресс", AppIcons.Chart),
)

@Composable
fun BoecRoot(nav: Nav, resumeTick: Int) {
    val state by Store.state.collectAsState()
    val dark = isDark(state.settings.theme)
    BoecTheme(dark) {
        SystemBars(dark)
        CompositionLocalProvider(LocalNav provides nav, LocalResume provides resumeTick) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AnimatedContent(state.onboarded, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) }, label = "onb") { onboarded ->
                    if (!onboarded) OnboardingScreen() else MainNav(nav)
                }
            }
        }
    }
}

@Composable
private fun MainNav(nav: Nav) {
    BackHandler(enabled = nav.stack.size > 1 || nav.tab != 0) { if (!nav.pop()) nav.tab = 0 }
    AnimatedContent(
        targetState = nav.top,
        transitionSpec = {
            val forward = nav.stack.contains(initialState) && targetState != Route.Tabs
            if (forward) (slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(260))) togetherWith (fadeOut(tween(180)))
            else (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.97f)) togetherWith (slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(200)))
        },
        label = "route",
    ) { r ->
        when (r) {
            Route.Tabs -> TabsScreen(nav)
            is Route.Player -> PlayerScreen(r.session)
            is Route.ExerciseDetail -> ExerciseDetailScreen(r.id)
            is Route.DrillDetail -> DrillDetailScreen(r.id)
            is Route.AnimalDetail -> AnimalDetailScreen(r.id)
            Route.Settings -> SettingsScreen()
            Route.Sources -> SourcesScreen()
            Route.EditProfile -> EditProfileScreen()
            Route.Safety -> SafetyScreen()
            is Route.EditReminder -> EditReminderScreen(r.id)
        }
    }
}

@Composable
private fun TabsScreen(nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = nav.tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { dir * it / 8 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(260)) { -dir * it / 8 } + fadeOut(tween(160)))
                },
                label = "tab",
            ) { t ->
                when (t) {
                    0 -> TodayScreen()
                    1 -> TrainScreen()
                    2 -> DefenseScreen()
                    3 -> RemindersScreen()
                    else -> ProgressScreen()
                }
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp, modifier = Modifier) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(
                    selected = nav.tab == i, onClick = { nav.tab = i },
                    icon = { Icon(t.icon, t.title, Modifier.size(24.dp)) },
                    label = { Text(t.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SystemBars(dark: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val w = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        val c = androidx.core.view.WindowCompat.getInsetsController(w, view)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }
}
