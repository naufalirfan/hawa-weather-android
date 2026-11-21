package com.naufal.hawaweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

        fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val city = prefs.getString("city_$id", "Jakarta") ?: "Jakarta"

            val views = RemoteViews(ctx.packageName, R.layout.widget_clock)

            val open = PendingIntent.getActivity(
                ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.root, open)

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
                    updatedViews.setOnClickPendingIntent(R.id.root, open)

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
