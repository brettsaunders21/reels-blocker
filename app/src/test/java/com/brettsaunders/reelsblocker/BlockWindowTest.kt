package com.brettsaunders.reelsblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BlockWindowTest {
    private val everyDay = 0b1111111
    private val weekdays = 0b0011111

    // 2026-10-26 is a Monday.
    private fun at(dayOfMonth: Int, hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, dayOfMonth, hour, minute) }

    @Test
    fun dayIndexStartsMonday() {
        assertEquals(0, BlockWindow.dayIndex(at(26, 12)))
        assertEquals(6, BlockWindow.dayIndex(at(1, 12).apply { set(Calendar.MONTH, Calendar.NOVEMBER) }))
    }

    @Test
    fun sameDayWindow() {
        val w = BlockWindow(1, 9 * 60, 17 * 60, everyDay)
        assertFalse(w.isActive(at(26, 8, 59)))
        assertTrue(w.isActive(at(26, 9, 0)))
        assertTrue(w.isActive(at(26, 16, 59)))
        assertFalse(w.isActive(at(26, 17, 0)))
    }

    @Test
    fun overnightWindowUsesStartDay() {
        // Friday 22:00 -> Saturday 07:00 only, for a weekday-only schedule.
        val w = BlockWindow(1, 22 * 60, 7 * 60, weekdays)
        assertTrue(w.isActive(at(30, 23)))  // Friday night
        assertTrue(w.isActive(at(31, 6)))   // Saturday early, started Friday
        assertFalse(w.isActive(at(31, 23))) // Saturday night
        assertFalse(w.isActive(at(26, 6)))  // Monday early, Sunday not selected
        assertTrue(w.isActive(at(27, 6)))   // Tuesday early, started Monday
    }

    @Test
    fun equalStartAndEndBlocksWholeDay() {
        val w = BlockWindow(1, 0, 0, weekdays)
        assertTrue(w.isActive(at(26, 0)))
        assertTrue(w.isActive(at(30, 23, 59)))
        assertFalse(w.isActive(at(31, 12)))
    }
}
