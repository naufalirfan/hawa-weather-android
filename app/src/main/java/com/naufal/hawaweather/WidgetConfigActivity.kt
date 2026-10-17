package com.naufal.hawaweather

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class WidgetConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 0)
        }
        root.addView(TextView(this).apply {
            text = "Pilih kota untuk widget"
            textSize = 20f
            gravity = Gravity.START
        })
        val names = HawaWidget.CITIES.keys.toList()
        root.addView(ListView(this).apply {
            adapter = ArrayAdapter(this@WidgetConfigActivity, android.R.layout.simple_list_item_1, names)
            setOnItemClickListener { _, _, pos, _ ->
                getSharedPreferences(HawaWidget.PREFS, MODE_PRIVATE).edit()
                    .putString("city_$widgetId", names[pos]).apply()
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                HawaWidget.render(this@WidgetConfigActivity, AppWidgetManager.getInstance(this@WidgetConfigActivity), widgetId)
                finish()
            }
        })
        setContentView(root)
    }
}
