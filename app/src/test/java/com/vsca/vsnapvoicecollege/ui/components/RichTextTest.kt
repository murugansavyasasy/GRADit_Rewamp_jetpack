package com.vsca.vsnapvoicecollege.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextTest {

    @Test
    fun wrapSelection_wrapsSelectedTextAndKeepsItSelected() {
        val result = wrapSelection("IA 2 begins", start = 0, end = 4, marker = "**")
        assertEquals("**IA 2** begins", result.text)
        assertEquals(2, result.selectionStart)
        assertEquals(6, result.selectionEnd)
    }

    @Test
    fun wrapSelection_onWrappedSelection_unwraps() {
        val result = wrapSelection("**IA 2** begins", start = 2, end = 6, marker = "**")
        assertEquals("IA 2 begins", result.text)
        assertEquals(0, result.selectionStart)
        assertEquals(4, result.selectionEnd)
    }

    @Test
    fun wrapSelection_withCursorOnly_insertsMarkerPairAroundCursor() {
        val result = wrapSelection("Hello ", start = 6, end = 6, marker = "_")
        assertEquals("Hello __", result.text)
        assertEquals(7, result.selectionStart)
        assertEquals(7, result.selectionEnd)
    }

    @Test
    fun toggleBullets_bulletsEverySelectedLine() {
        val text = "Bring ID card\nReach by 9:40\nCarry pens"
        val result = toggleBullets(text, start = 3, end = 20)
        assertEquals("• Bring ID card\n• Reach by 9:40\nCarry pens", result.text)
    }

    @Test
    fun toggleBullets_removesBulletsWhenAllLinesHaveThem() {
        val result = toggleBullets("• One\n• Two", start = 0, end = 11)
        assertEquals("One\nTwo", result.text)
    }

    @Test
    fun insertLink_turnsSelectionIntoMarkdownLink() {
        val result = insertLink("See the timetable", start = 8, end = 17, url = "https://college.edu/tt")
        assertEquals("See the [timetable](https://college.edu/tt)", result.text)
        assertEquals(result.text.length, result.selectionStart)
    }

    @Test
    fun insertLink_withoutSelection_insertsBareUrlAtCursor() {
        val result = insertLink("Link: ", start = 6, end = 6, url = "https://a.in")
        assertEquals("Link: https://a.in", result.text)
    }
}
