package com.naufal.hawaweather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class HawaWidget : AppWidgetProvider() {

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
                remove("temp_$it")
                remove("icon_$it")
                remove("detail_$it")
            }
        }.apply()
    }

    companion object {
        const val PREFS = "hawa_widget"
        const val ACTION_REFRESH = "com.naufal.hawaweather.ACTION_REFRESH_WIDGET"

        val CITIES = linkedMapOf(
            "Jakarta" to (-6.2088 to 106.8456),
            "Yogyakarta" to (-7.7956 to 110.3695),
            "Bandung" to (-6.9175 to 107.6191),
            "Surabaya" to (-7.2575 to 112.7521),
            "Bali" to (-8.4095 to 115.1889),
        )

        fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val city = prefs.getString("city_$id", null)

            val views = RemoteViews(ctx.packageName, R.layout.widget_hawa)

            val open = PendingIntent.getActivity(
                ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.root, open)

            if (city == null) {
                views.setTextViewText(R.id.city, "Ketuk untuk atur")
                views.setTextViewText(R.id.temp, "--°")
                views.setTextViewText(R.id.icon, "🌡️")
                views.setTextViewText(R.id.detail, "Pilih kota")
                mgr.updateAppWidget(id, views)
                return
            }

            views.setTextViewText(R.id.city, city)

            // Step 1: Render cached data immediately so UI is never stuck on --°
            val cachedTemp = prefs.getString("temp_$id", null)
            val cachedIcon = prefs.getString("icon_$id", null)
            val cachedDetail = prefs.getString("detail_$id", null)

            if (cachedTemp != null) {
                views.setTextViewText(R.id.temp, cachedTemp)
                views.setTextViewText(R.id.icon, cachedIcon ?: "🌤️")
                views.setTextViewText(R.id.detail, cachedDetail ?: "Tersimpan")
            } else {
                views.setTextViewText(R.id.temp, "…°")
                views.setTextViewText(R.id.icon, "🌤️")
                views.setTextViewText(R.id.detail, "Memperbarui data...")
            }
            mgr.updateAppWidget(id, views)

            // Step 2: Fetch fresh live weather on background thread
            thread {
                try {
                    val coords = CITIES[city] ?: return@thread
                    val cur = Weather.fetch(coords.first, coords.second)
                    val updatedViews = RemoteViews(ctx.packageName, R.layout.widget_hawa)
                    updatedViews.setOnClickPendingIntent(R.id.root, open)
                    updatedViews.setTextViewText(R.id.city, city)

                    if (cur != null) {
                        val tempStr = "${Math.round(cur.temp)}°"
                        val iconStr = Weather.icon(cur.code)
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val detailStr = "Terasa ${Math.round(cur.feels)}° · $timeStr"

                        prefs.edit()
                            .putString("temp_$id", tempStr)
                            .putString("icon_$id", iconStr)
                            .putString("detail_$id", detailStr)
                            .apply()

                        updatedViews.setTextViewText(R.id.temp, tempStr)
                        updatedViews.setTextViewText(R.id.icon, iconStr)
                        updatedViews.setTextViewText(R.id.detail, detailStr)
                    } else {
                        // Network error: preserve cache if present
                        val prev = prefs.getString("temp_$id", null)
                        if (prev != null) {
                            updatedViews.setTextViewText(R.id.temp, prev)
                            updatedViews.setTextViewText(R.id.icon, prefs.getString("icon_$id", "🌤️"))
                            updatedViews.setTextViewText(R.id.detail, "${prefs.getString("detail_$id", "Tersimpan")} (offline)")
                        } else {
                            updatedViews.setTextViewText(R.id.temp, "--°")
                            updatedViews.setTextViewText(R.id.icon, "📡")
                            updatedViews.setTextViewText(R.id.detail, "Ketuk untuk perbarui")
                        }
                    }
                    mgr.updateAppWidget(id, updatedViews)
                } catch (e: Exception) {
                    android.util.Log.e("HawaWidget", "Fetch error for widget $id", e)
                }
            }
        }

        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, HawaWidget::class.java))
            ids.forEach { render(ctx, mgr, it) }
        }
    }
}
