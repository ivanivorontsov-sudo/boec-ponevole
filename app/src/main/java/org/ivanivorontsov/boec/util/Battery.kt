package org.ivanivorontsov.boec.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/** Battery-optimisation helpers with OEM-specific guidance (see dontkillmyapp.com). */
object Battery {
    val maker: String get() = Build.MANUFACTURER.lowercase()
    val isOppoFamily: Boolean get() = maker in setOf("realme", "oppo", "oneplus")

    fun isIgnoring(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    @SuppressLint("BatteryLife")
    fun requestIgnore(ctx: Context) {
        val ok = runCatching {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!ok) runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openAppDetails(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private val autostart = listOf(
        // realme / OPPO / OnePlus (ColorOS, realme UI)
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
        // Xiaomi / Redmi / POCO
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        // Huawei / Honor
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        // vivo
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        // Samsung battery
        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
    )

    /** Opens the OEM auto-start / background screen if one exists, otherwise the app details page. */
    fun openOemSettings(ctx: Context) {
        for (c in autostart) {
            if (!maker.let { m -> c.packageName.contains(m) || (isOppoFamily && (c.packageName.contains("coloros") || c.packageName.contains("oppo"))) || (m == "xiaomi" && c.packageName.contains("miui")) || (m == "honor" && c.packageName.contains("honor")) }) continue
            val ok = runCatching { ctx.startActivity(Intent().setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
            if (ok) return
        }
        openAppDetails(ctx)
    }

    fun oemSteps(): Pair<String, List<String>> = when {
        isOppoFamily -> "realme / OPPO / OnePlus (realme UI, ColorOS)" to listOf(
            "Настройки → Приложения → Управление приложениями → «Боец поневоле» → Использование батареи: включи «Разрешить фоновую активность» и «Разрешить автозапуск».",
            "Там же «Оптимизация батареи» → «Не оптимизировать» (или Настройки → Батарея → Дополнительно → Оптимизация использования батареи).",
            "Открой «Недавние», потяни карточку приложения вниз (или ⋮) → «Заблокировать», чтобы система её не выгружала.",
            "Уведомления: разреши всё, включая «Баннеры» и «На экране блокировки».",
            "Android 14+: Настройки → Приложения → Специальный доступ → «Полноэкранные уведомления» → включи для «Боец поневоле».",
            "Если звонки не приходят в режиме энергосбережения — добавь приложение в исключения или выключи «Сверхэнергосбережение».",
        )
        maker == "xiaomi" || maker == "redmi" || maker == "poco" -> "Xiaomi / Redmi / POCO (HyperOS, MIUI)" to listOf(
            "Безопасность → Разрешения → Автозапуск → включи «Боец поневоле».",
            "Настройки → Приложения → «Боец поневоле» → Контроль активности → «Нет ограничений».",
            "Разреши «Экран блокировки» и «Всплывающие окна» в разрешениях приложения.",
            "Закрепи приложение в «Недавних» (замок).",
        )
        maker == "samsung" -> "Samsung (One UI)" to listOf(
            "Настройки → Приложения → «Боец поневоле» → Батарея → «Без ограничений».",
            "Настройки → Обслуживание устройства → Батарея → Ограничения в фоне: убери приложение из «спящих».",
        )
        maker == "huawei" || maker == "honor" -> "Huawei / Honor" to listOf(
            "Настройки → Батарея → Запуск приложений → «Боец поневоле» → вручную, включи все три переключателя.",
        )
        else -> "Android" to listOf(
            "Настройки → Приложения → «Боец поневоле» → Батарея → «Без ограничений».",
            "Разреши уведомления и полноэкранные уведомления.",
        )
    }
}
