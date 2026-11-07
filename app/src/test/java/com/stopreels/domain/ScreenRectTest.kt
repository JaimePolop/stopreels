package com.stopreels.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenRectTest {
    @Test
    fun expandToKeepsBoundsInsideScreen() {
        val screen = ScreenRect(0, 0, 1080, 2400)
        val result = ScreenRect(10, 10, 40, 40).expandTo(180, 120, screen)

        assertEquals(0, result.left)
        assertEquals(0, result.top)
        assertEquals(180, result.right)
        assertEquals(120, result.bottom)
    }
}
