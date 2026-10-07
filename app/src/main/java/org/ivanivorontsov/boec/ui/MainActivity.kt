package org.ivanivorontsov.boec.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import org.ivanivorontsov.boec.alarm.AlarmScheduler
import org.ivanivorontsov.boec.data.Store

class MainActivity : ComponentActivity() {
    private val nav = Nav()
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferHighRefreshRate()
        handle(intent)
        setContent { BoecRoot(nav, resumeTick.intValue) }
        if (Store.value.onboarded) AlarmScheduler.rescheduleAll(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    private fun handle(i: Intent?) {
        val day = i?.getIntExtra(EXTRA_START_DAY, 0) ?: 0
        if (day > 0 && Store.value.onboarded) {
            val d = Store.plan().days.firstOrNull { it.isoDay == day }
            if (d != null) nav.openPlayer(d.session)
            i?.removeExtra(EXTRA_START_DAY)
        }
        val tab = i?.getIntExtra(EXTRA_TAB, -1) ?: -1
        if (tab >= 0) nav.tab = tab
    }

    /** Ask for the panel's highest refresh rate (e.g. 120/144 Hz on realme) at the current resolution. */
    private fun preferHighRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val d = display ?: return
        val cur = d.mode
        val best = d.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (best.modeId != cur.modeId || window.attributes.preferredDisplayModeId != best.modeId) {
            window.attributes = window.attributes.also { it.preferredDisplayModeId = best.modeId }
        }
    }

    companion object {
        const val EXTRA_START_DAY = "start_day"
        const val EXTRA_REMINDER = "reminder"
        const val EXTRA_TAB = "tab"
    }
}
