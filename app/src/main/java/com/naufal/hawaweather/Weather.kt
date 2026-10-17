package com.naufal.hawaweather

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Current(val temp: Double, val code: Int, val feels: Double)

object Weather {
    /** WMO weather code -> emoji. */
    fun icon(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67 -> "🌧️"
        71, 73, 75, 77 -> "❄️"
        80, 81, 82 -> "🌦️"
        85, 86 -> "🌨️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    /** Blocking fetch; call off the main thread. Returns null on any failure. */
    fun fetch(lat: Double, lon: Double): Current? = try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,weather_code&timezone=auto"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "HawaWeatherWidget/1.0")
        }
        conn.inputStream.bufferedReader().use { r ->
            val cur = JSONObject(r.readText()).getJSONObject("current")
            Current(
                temp = cur.getDouble("temperature_2m"),
                code = cur.getInt("weather_code"),
                feels = cur.getDouble("apparent_temperature"),
            )
        }.also { conn.disconnect() }
    } catch (e: Exception) {
        null
    }
}
