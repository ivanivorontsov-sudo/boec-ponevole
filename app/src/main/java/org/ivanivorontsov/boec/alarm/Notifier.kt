package org.ivanivorontsov.boec.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.ivanivorontsov.boec.R
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.domain.AlarmPlan
import org.ivanivorontsov.boec.model.Reminder
import org.ivanivorontsov.boec.model.ReminderKind
import org.ivanivorontsov.boec.ui.CallActivity
import org.ivanivorontsov.boec.ui.MainActivity

object Notifier {
    const val CH_REMINDERS = "reminders"
    const val CH_CALLS = "calls_v1"
    const val CH_MISSED = "missed"
    const val TEST_ID = "test_call"
    private const val ACCENT = 0xFFFF6A3D.toInt()

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val reminders = NotificationChannel(CH_REMINDERS, "Напоминания", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Вода, сон, осанка, клещи, аптечка и тренировки в режиме уведомления"
            enableVibration(true)
        }
        val ringtone: Uri? = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val calls = NotificationChannel(CH_CALLS, "Звонки-напоминания", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Полноэкранный «входящий звонок» о тренировке"
            setSound(ringtone, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 600, 800, 600)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val missed = NotificationChannel(CH_MISSED, "Пропущенные", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannels(listOf(reminders, calls, missed))
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun canFullScreen(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun notifId(reminderId: String) = AlarmPlan.requestCode(reminderId, false)

    private fun action(ctx: Context, action: String, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, (action + id).hashCode(),
            Intent(ctx, ActionReceiver::class.java).setAction(action).putExtra(AlarmScheduler.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun openApp(ctx: Context, r: Reminder?, start: Boolean): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (r != null) {
            i.putExtra(MainActivity.EXTRA_REMINDER, r.id)
            if (start && r.planDay > 0) i.putExtra(MainActivity.EXTRA_START_DAY, r.planDay)
        }
        return PendingIntent.getActivity(ctx, ((r?.id ?: "") + start).hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun showReminder(ctx: Context, r: Reminder) {
        if (!canPost(ctx)) return
        val snoozeMin = Store.value.settings.snoozeMin
        val isWorkout = r.kind == ReminderKind.WORKOUT || r.kind == ReminderKind.SKILL
        val b = NotificationCompat.Builder(ctx, CH_REMINDERS)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(ACCENT)
            .setContentTitle(if (isWorkout) "Тренировка: ${r.title}" else r.title)
            .setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setCategory(if (isWorkout) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, r, isWorkout))
        if (isWorkout) {
            b.addAction(0, "Начать", openApp(ctx, r, true))
            b.addAction(0, "Отложить $snoozeMin мин", action(ctx, ActionReceiver.ACTION_SNOOZE, r.id))
            b.addAction(0, "Пропустить", action(ctx, ActionReceiver.ACTION_SKIP, r.id))
        } else {
            b.addAction(0, if (r.kind == ReminderKind.WATER) "Выпил(а)" else "Готово", action(ctx, ActionReceiver.ACTION_DONE, r.id))
            b.addAction(0, "Отложить $snoozeMin мин", action(ctx, ActionReceiver.ACTION_SNOOZE, r.id))
        }
        notify(ctx, notifId(r.id), b.build())
    }

    fun callIntent(ctx: Context, r: Reminder, preview: Boolean = false): Intent =
        Intent(ctx, CallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            .putExtra(CallActivity.EXTRA_ID, r.id)
            .putExtra(CallActivity.EXTRA_TITLE, r.title)
            .putExtra(CallActivity.EXTRA_TEXT, r.text)
            .putExtra(CallActivity.EXTRA_PREVIEW, preview)

    fun showCall(ctx: Context, r: Reminder) {
        val settings = Store.value.settings
        val full = PendingIntent.getActivity(ctx, notifId(r.id), callIntent(ctx, r), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!canPost(ctx)) {
            // Without notification permission a full-screen intent cannot be posted; try to open directly (allowed only in some states).
            runCatching { ctx.startActivity(callIntent(ctx, r)) }
            return
        }
        val accept = PendingIntent.getActivity(
            ctx, notifId(r.id) + 7, callIntent(ctx, r).putExtra(CallActivity.EXTRA_AUTO_ACCEPT, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(ctx, CH_CALLS)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(ACCENT)
            .setColorized(true)
            .setContentTitle("Входящий: ${r.title}")
            .setContentText(r.text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .setTimeoutAfter(settings.callTimeoutSec * 1000L)
            .addAction(0, "Отклонить (+${settings.snoozeMin} мин)", action(ctx, ActionReceiver.ACTION_SNOOZE, r.id))
            .addAction(0, "Принять", accept)
        if (!settings.vibrate) b.setVibrate(longArrayOf(0))
        val n = b.build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        notify(ctx, notifId(r.id), n)
    }

    fun showMissed(ctx: Context, r: Reminder) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(ACCENT)
            .setContentTitle("Пропущенный звонок: ${r.title}")
            .setContentText("Перезвоню через ${Store.value.settings.snoozeMin} мин. Или начни сейчас.")
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, r, true))
            .addAction(0, "Начать", openApp(ctx, r, true))
            .addAction(0, "Пропустить", action(ctx, ActionReceiver.ACTION_SKIP, r.id))
            .build()
        notify(ctx, notifId(r.id) + 1, n)
    }

    private fun notify(ctx: Context, id: Int, n: Notification) {
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }

    fun cancel(ctx: Context, reminderId: String) {
        val nm = NotificationManagerCompat.from(ctx)
        nm.cancel(notifId(reminderId)); nm.cancel(notifId(reminderId) + 1)
    }
}
