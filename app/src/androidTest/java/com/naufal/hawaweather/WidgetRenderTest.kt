package com.naufal.hawaweather

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import android.widget.RemoteViews

/**
 * RemoteViews hanya mendukung subset view/atribut. apply() memakai mesin RemoteViews
 * sungguhan, jadi test ini gagal kalau layout widget memakai view yang tidak didukung
 * (penyebab widget force-close saat dipasang di home screen).
 */
@RunWith(AndroidJUnit4::class)
class WidgetRenderTest {

    private val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun widgetLayoutAppliesAndTextIdsResolve() {
        val views = RemoteViews(ctx.packageName, R.layout.widget_hawa)
        views.setTextViewText(R.id.temp, "31°")
        views.setTextViewText(R.id.city, "Yogyakarta")
        views.setTextViewText(R.id.icon, "☀️")
        views.setTextViewText(R.id.detail, "Terasa 34° · 07:15")

        val root = views.apply(ctx, FrameLayout(ctx))

        assertNotNull(root)
        assertEquals("31°", root.findViewById<TextView>(R.id.temp).text.toString())
        assertEquals("Yogyakarta", root.findViewById<TextView>(R.id.city).text.toString())
    }
}
