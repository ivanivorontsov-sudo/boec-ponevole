package org.ivanivorontsov.boec.ui

import android.app.KeyguardManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.ivanivorontsov.boec.alarm.ActionReceiver
import org.ivanivorontsov.boec.alarm.AlarmScheduler
import org.ivanivorontsov.boec.alarm.Notifier
import org.ivanivorontsov.boec.data.Store
import org.ivanivorontsov.boec.model.ReminderKind

/** Full-screen "incoming call" reminder: rings, vibrates, accept = open workout, decline = snooze. */
class CallActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var handled = false
    private lateinit var reminderId: String
    private var preview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        } else { setShowWhenLocked(true); setTurnScreenOn(true) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        reminderId = intent.getStringExtra(EXTRA_ID).orEmpty()
        preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()

        if (intent.getBooleanExtra(EXTRA_AUTO_ACCEPT, false)) { accept(); return }

        // Take over the sound from the insistent notification.
        Notifier.cancel(this, reminderId)
        startRinging()
        val timeout = Store.value.settings.callTimeoutSec
        handler.postDelayed({ if (!handled) missed() }, timeout * 1000L)

        setContent {
            BoecTheme(dark = true) {
                CallScreen(title, text, Store.value.settings.snoozeMin, timeout, onAccept = ::accept, onDecline = ::decline)
            }
        }
    }

    private fun startRinging() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = runCatching { RingtoneManager.getRingtone(this, uri) }.getOrNull()?.apply {
            audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            runCatching { play() }
        }
        if (Store.value.settings.vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
            else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600, 800, 1200), 0))
        }
    }

    private fun stopRinging() {
        runCatching { ringtone?.stop() }; ringtone = null
        runCatching { vibrator?.cancel() }; vibrator = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun accept() {
        handled = true
        stopRinging()
        Notifier.cancel(this, reminderId)
        if (!preview) {
            Store.update { s -> s.copy(snoozes = s.snoozes.filter { it.reminderId != reminderId }) }
            AlarmScheduler.rescheduleAll(this)
        }
        val r = Store.value.reminders.firstOrNull { it.id == reminderId }
        val open = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (r != null && (r.kind == ReminderKind.WORKOUT || r.kind == ReminderKind.SKILL) && r.planDay > 0) open.putExtra(MainActivity.EXTRA_START_DAY, r.planDay)
        else if (r != null && r.id != Notifier.TEST_ID) open.putExtra(MainActivity.EXTRA_TAB, 3)
        val km = getSystemService(KeyguardManager::class.java)
        if (km.isKeyguardLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { startActivity(open); finish() }
                override fun onDismissCancelled() { finish() }
                override fun onDismissError() { finish() }
            })
        } else { if (!preview) startActivity(open); finish() }
    }

    private fun decline() {
        handled = true
        stopRinging()
        if (!preview && reminderId != Notifier.TEST_ID) {
            ActionReceiver.snooze(this, reminderId)
            AlarmScheduler.rescheduleAll(this)
        }
        finish()
    }

    private fun missed() {
        handled = true
        stopRinging()
        if (!preview) Store.value.reminders.firstOrNull { it.id == reminderId }?.let { if (it.id != Notifier.TEST_ID) Notifier.showMissed(this, it) }
        finish()
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_PREVIEW = "preview"
        const val EXTRA_AUTO_ACCEPT = "auto_accept"
    }
}

@Composable
private fun CallScreen(title: String, text: String, snoozeMin: Int, timeoutSec: Int, onAccept: () -> Unit, onDecline: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "call")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")
    val bob by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    var left by remember { mutableIntStateOf(timeoutSec) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A120C), Color(0xFF120C0A), Color(0xFF07080A))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            Text("Входящий вызов · Боец поневоле", color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(48.dp))
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                for (k in 0..2) {
                    val p = (ring + k / 3f) % 1f
                    Box(Modifier.size(220.dp).graphicsLayer { scaleX = 0.5f + p * 0.6f; scaleY = 0.5f + p * 0.6f; alpha = (1f - p) * 0.5f }.clip(CircleShape).background(Ember))
                }
                Box(Modifier.size(120.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF7A45), EmberDeep))), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Dumbbell, null, tint = Color.White, modifier = Modifier.size(60.dp))
                }
            }
            Spacer(Modifier.height(36.dp))
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
            Spacer(Modifier.height(10.dp))
            Text(text, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text("Сброс через $left с", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallButton(Icons.Default.Close, "Отложить\n$snoozeMin мин", Danger, 0f, onDecline)
                CallButton(Icons.Default.Call, "Принять\nи начать", Color(0xFF22C55E), bob, onAccept)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CallButton(icon: ImageVector, label: String, color: Color, bob: Float, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.graphicsLayer { translationY = -bob * 14f; rotationZ = bob * 12f }.size(78.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}
