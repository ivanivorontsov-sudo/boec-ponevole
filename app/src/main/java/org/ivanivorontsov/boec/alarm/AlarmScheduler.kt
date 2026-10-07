package org.ivanivorontsov.boec.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.AlarmPlan
import org.ivanivorontsov.boec.domain.ScheduledAlarm
import org.ivanivorontsov.boec.model.AlertMode
import org.ivanivorontsov.boec.ui.MainActivity
import java.time.ZoneId

object AlarmScheduler {
    const val ACTION_FIRE = "org.ivanivorontsov.boec.ALARM"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_SNOOZE = "snooze"
    const val EXTRA_COUNT = "count"
    private const val PREFS = "alarms"
    private const val KEY = "registered"

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = ctx.getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    private fun intentFor(ctx: Context, rc: Int, id: String = "", snooze: Boolean = false, count: Int = 0) =
        Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("boec://alarm/$rc"))
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_SNOOZE, snooze)
            .putExtra(EXTRA_COUNT, count)

    /** Cancels everything registered before and registers the next trigger of every enabled reminder + pending snoozes. */
    fun rescheduleAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet(KEY, emptySet())!!.forEach { s ->
            val rc = s.toIntOrNull() ?: return@forEach
            PendingIntent.getBroadcast(ctx, rc, intentFor(ctx, rc), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { am.cancel(it); it.cancel() }
        }
        val s = Store.value
        val now = System.currentTimeMillis()
        // Drop stale snoozes.
        if (s.snoozes.any { it.atMillis <= now - 60_000 }) Store.update { st -> st.copy(snoozes = st.snoozes.filter { it.atMillis > now - 60_000 }) }
        val plan = AlarmPlan.compute(Store.value.reminders, Store.value.snoozes, now, ZoneId.systemDefault())
        val counts = Store.value.snoozes.associate { it.reminderId to it.count }
        val registered = HashSet<String>()
        for (a in plan) {
            set(ctx, am, a, if (a.snooze) counts[a.reminderId] ?: 0 else 0)
            registered += a.requestCode.toString()
        }
        prefs.edit().putStringSet(KEY, registered).apply()
    }

    private fun set(ctx: Context, am: AlarmManager, a: ScheduledAlarm, count: Int) {
        val pi = PendingIntent.getBroadcast(
            ctx, a.requestCode, intentFor(ctx, a.requestCode, a.reminderId, a.snooze, count),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canExact(ctx)) {
                if (a.mode == AlertMode.CALL) {
                    // Alarm-clock alarms are exact, exempt from Doze and shown in the status bar.
                    val show = PendingIntent.getActivity(
                        ctx, 1, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(a.atMillis, show), pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, a.atMillis, pi)
                }
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, a.atMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, a.atMillis, pi)
        }
    }

    fun nextAlarm(): ScheduledAlarm? =
        AlarmPlan.compute(Store.value.reminders, Store.value.snoozes, System.currentTimeMillis(), ZoneId.systemDefault()).firstOrNull()
}
