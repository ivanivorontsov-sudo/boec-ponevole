package org.ivanivorontsov.boec.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import org.ivanivorontsov.boec.domain.Dates
import org.ivanivorontsov.boec.domain.ScheduleGenerator
import org.ivanivorontsov.boec.model.AppState
import org.ivanivorontsov.boec.model.WeekPlan
import java.io.File
import java.time.LocalDate

/** Tiny JSON-file store. All data stays on the device (no network permission). */
object Store {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true }
    private lateinit var file: File
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> get() = _state
    private val lock = Any()
    private var initialized = false

    fun init(ctx: Context) = synchronized(lock) {
        if (initialized) return
        file = File(ctx.filesDir, "state.json")
        _state.value = runCatching { json.decodeFromString(AppState.serializer(), file.readText()) }.getOrElse { AppState() }
        initialized = true
    }

    val value: AppState get() = _state.value

    fun update(block: (AppState) -> AppState): AppState = synchronized(lock) {
        val next = block(_state.value)
        _state.value = next
        val tmp = File(file.parentFile, "state.json.tmp")
        tmp.writeText(json.encodeToString(AppState.serializer(), next))
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        next
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun weekIndex(s: AppState = value, today: Long = todayEpochDay()): Int {
        val start = if (s.blockStartEpochDay == 0L) Dates.mondayOf(today) else s.blockStartEpochDay
        return ((Dates.mondayOf(today) - start) / 7).toInt().coerceAtLeast(0)
    }

    private var cacheKey: Any? = null
    private var cachePlan: WeekPlan? = null

    fun plan(s: AppState = value): WeekPlan = synchronized(lock) {
        val key = s.profile to weekIndex(s)
        if (key != cacheKey || cachePlan == null) {
            cachePlan = ScheduleGenerator.generate(s.profile, weekIndex(s))
            cacheKey = key
        }
        cachePlan!!
    }
}
