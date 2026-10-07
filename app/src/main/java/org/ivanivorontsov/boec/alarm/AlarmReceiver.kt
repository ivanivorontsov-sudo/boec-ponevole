package org.ivanivorontsov.boec.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.AlarmPlan
import org.ivanivorontsov.boec.model.AlertMode
import org.ivanivorontsov.boec.model.ReminderKind
import org.ivanivorontsov.boec.model.Snooze

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID).orEmpty()
        val snooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)
        val count = intent.getIntExtra(AlarmScheduler.EXTRA_COUNT, 0)
        val s = Store.value
        val r = s.reminders.firstOrNull { it.id == id }
        if (snooze) Store.update { st -> st.copy(snoozes = st.snoozes.filter { it.reminderId != id }) }
        if (r != null && (r.enabled || snooze)) {
            val trainedToday = s.log.any { it.epochDay == Store.todayEpochDay() && it.completed }
            val skip = (r.kind == ReminderKind.WORKOUT || r.kind == ReminderKind.SKILL) && trainedToday && !snooze
            if (!skip) {
                if (r.mode == AlertMode.CALL) {
                    Notifier.showCall(ctx, r)
                    // Unanswered call rings again later (max 2 repeats). Accept/decline replaces this.
                    if (count < 2 && r.id != Notifier.TEST_ID) {
                        val at = System.currentTimeMillis() + s.settings.callTimeoutSec * 1000L + s.settings.snoozeMin * 60_000L
                        Store.update { st -> st.copy(snoozes = st.snoozes.filter { it.reminderId != id } + Snooze(id, at, count + 1)) }
                    }
                } else {
                    Notifier.showReminder(ctx, r)
                }
            }
        }
        AlarmScheduler.rescheduleAll(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        AlarmScheduler.rescheduleAll(ctx)
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID).orEmpty()
        Notifier.cancel(ctx, id)
        when (intent.action) {
            ACTION_SNOOZE -> snooze(ctx, id)
            ACTION_SKIP, ACTION_DONE -> Store.update { st -> st.copy(snoozes = st.snoozes.filter { it.reminderId != id }) }
        }
        AlarmScheduler.rescheduleAll(ctx)
    }

    companion object {
        const val ACTION_SNOOZE = "org.ivanivorontsov.boec.SNOOZE"
        const val ACTION_SKIP = "org.ivanivorontsov.boec.SKIP"
        const val ACTION_DONE = "org.ivanivorontsov.boec.DONE"

        fun snooze(ctx: Context, id: String) {
            val now = System.currentTimeMillis()
            Store.update { st ->
                val at = AlarmPlan.snoozeUntil(now, st.settings.snoozeMin)
                st.copy(snoozes = st.snoozes.filter { it.reminderId != id } + Snooze(id, at, 0))
            }
        }
    }
}
