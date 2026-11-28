package com.naufal.hawaweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import kotlin.concurrent.thread

class HawaClockWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(ctx, mgr, it) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_REFRESH) {
            refreshAll(ctx)
        }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            ids.forEach {
                remove("city_$it")
                remove("weather_info_$it")
            }
        }.apply()
    }

    companion object {
        const val PREFS = "hawa_clock_widget"
        const val ACTION_REFRESH = "com.naufal.hawaweather.ACTION_REFRESH_CLOCK_WIDGET"

        val CITIES = linkedMapOf(
            "Jakarta" to (-6.2088 to 106.8456),
            "Yogyakarta" to (-7.7956 to 110.3695),
            "Bandung" to (-6.9175 to 107.6191),
            "Surabaya" to (-7.2575 to 112.7521),
            "Bali" to (-8.4095 to 115.1889),
        )

        /**
         * Resolves the default Clock / Alarm app across various OEM manufacturers (Oppo, Realme, Samsung, Xiaomi, Pixel, etc.)
         */
        fun getClockIntent(ctx: Context): Intent {
            val pm = ctx.packageManager

            // 1. Standard Android AlarmClock.ACTION_SHOW_ALARMS
            val showAlarmsIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (showAlarmsIntent.resolveActivity(pm) != null) {
                return showAlarmsIntent
            }

            // 2. Known OEM Clock package launcher intents
            val clockPackages = listOf(
                "com.coloros.alarm",                 // Oppo / Realme ColorOS
                "com.oplus.alarmclock",              // OnePlus / Oppo
                "com.google.android.deskclock",      // Google Pixel / AOSP
                "com.sec.android.app.clockpackage",  // Samsung One UI
                "com.android.deskclock",             // Xiaomi HyperOS/MIUI / AOSP
                "com.oneplus.deskclock",             // OnePlus
                "com.vivo.alarmclock",               // Vivo / iQOO FuntouchOS
                "com.android.BBKClock",              // Vivo BBK
                "com.huawei.deskclock",              // Huawei EMUI
                "com.hihonor.deskclock",             // Honor MagicOS
                "com.transsion.deskclock",           // Transsion (Infinix, Tecno, itel)
                "com.asus.deskclock",                // Asus ZenUI
                "com.motorola.blur.alarmclock"       // Motorola
            )

            for (pkg in clockPackages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return launchIntent
                }
            }

            // 3. Fallback to ACTION_SET_ALARM
            val setAlarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (setAlarmIntent.resolveActivity(pm) != null) {
                return setAlarmIntent
            }

            return Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        private fun attachClickIntents(views: RemoteViews, ctx: Context, id: Int) {
            val clockIntent = getClockIntent(ctx)
            val openClock = PendingIntent.getActivity(
                ctx,
                id * 100 + 1,
                clockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val openHawa = PendingIntent.getActivity(
                ctx,
                id * 100 + 2,
                Intent(ctx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            // Saat diklik tanggal atau jam -> mengarah ke aplikasi jam bawaan HP
            views.setOnClickPendingIntent(R.id.root, openClock)
            views.setOnClickPendingIntent(R.id.date, openClock)
            views.setOnClickPendingIntent(R.id.clock_container, openClock)
            views.setOnClickPendingIntent(R.id.clock_hour, openClock)
            views.setOnClickPendingIntent(R.id.clock_minute, openClock)

            // Saat diklik cuaca -> mengarah ke aplikasi Hawa
            views.setOnClickPendingIntent(R.id.weather_info, openHawa)
        }

        fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val city = prefs.getString("city_$id", "Jakarta") ?: "Jakarta"

            val views = RemoteViews(ctx.packageName, R.layout.widget_clock)
            attachClickIntents(views, ctx, id)

            // Step 1: Render cached data immediately
            val cachedWeather = prefs.getString("weather_info_$id", null)
            if (cachedWeather != null) {
                views.setTextViewText(R.id.weather_info, cachedWeather)
            } else {
                views.setTextViewText(R.id.weather_info, "Memuat cuaca... 🌤️")
            }
            mgr.updateAppWidget(id, views)

            // Step 2: Fetch fresh weather in background thread
            thread {
                try {
                    val coords = CITIES[city] ?: (-6.2088 to 106.8456)
                    val cur = Weather.fetch(coords.first, coords.second)
                    val updatedViews = RemoteViews(ctx.packageName, R.layout.widget_clock)
                    attachClickIntents(updatedViews, ctx, id)

                    if (cur != null) {
                        val condition = Weather.conditionName(cur.code)
                        val icon = Weather.icon(cur.code)
                        val temp = Math.round(cur.temp)
                        val weatherStr = "$condition $icon $temp°"

                        prefs.edit()
                            .putString("weather_info_$id", weatherStr)
                            .apply()

                        updatedViews.setTextViewText(R.id.weather_info, weatherStr)
                    } else {
                        val prev = prefs.getString("weather_info_$id", null)
                        if (prev != null) {
                            updatedViews.setTextViewText(R.id.weather_info, prev)
                        } else {
                            updatedViews.setTextViewText(R.id.weather_info, "Ketuk untuk perbarui 📡")
                        }
                    }
                    mgr.updateAppWidget(id, updatedViews)
                } catch (e: Exception) {
                    android.util.Log.e("HawaClockWidget", "Fetch error for widget $id", e)
                }
            }
        }

        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, HawaClockWidget::class.java))
            ids.forEach { render(ctx, mgr, it) }
        }
    }
}
