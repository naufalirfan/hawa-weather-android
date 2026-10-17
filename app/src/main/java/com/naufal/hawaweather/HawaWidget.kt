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
        val pending = goAsync()
        thread {
            try {
                ids.forEach { render(ctx, mgr, it) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            ids.forEach { remove("city_$it") }
        }.apply()
    }

    companion object {
        const val PREFS = "hawa_widget"
        val CITIES = linkedMapOf(
            "Jakarta" to (-6.2088 to 106.8456),
            "Yogyakarta" to (-7.7956 to 110.3695),
            "Bandung" to (-6.9175 to 107.6191),
            "Surabaya" to (-7.2575 to 112.7521),
            "Bali" to (-8.4095 to 115.1889),
        )

        fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val views = RemoteViews(ctx.packageName, R.layout.widget_hawa)
            val city = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("city_$id", null)

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

            val (lat, lon) = CITIES[city] ?: return
            views.setTextViewText(R.id.city, city)
            val cur = Weather.fetch(lat, lon)
            if (cur == null) {
                views.setTextViewText(R.id.temp, "--°")
                views.setTextViewText(R.id.icon, "📡")
                views.setTextViewText(R.id.detail, "Gagal memuat")
            } else {
                views.setTextViewText(R.id.temp, "${cur.temp.toInt()}°")
                views.setTextViewText(R.id.icon, Weather.icon(cur.code))
                views.setTextViewText(
                    R.id.detail,
                    "Terasa ${cur.feels.toInt()}° · " +
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                )
            }
            mgr.updateAppWidget(id, views)
        }

        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, HawaWidget::class.java))
            ids.forEach { render(ctx, mgr, it) }
        }
    }
}
