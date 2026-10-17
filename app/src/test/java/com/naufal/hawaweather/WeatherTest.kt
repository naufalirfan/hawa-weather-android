package com.naufal.hawaweather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTest {
    @Test
    fun clearSkyIsSun() = assertEquals("☀️", Weather.icon(0))

    @Test
    fun thunderIsStorm() = assertEquals("⛈️", Weather.icon(95))

    @Test
    fun unknownCodeFallsBack() = assertEquals("🌡️", Weather.icon(1234))
}
