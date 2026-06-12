package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalKeysGeometryTest {

    private val esc = 27.toChar()

    @Test
    fun `arrow keys map to standard CSI sequences`() {
        assertEquals("$esc[A", TerminalKeys.sequenceOf("↑"))
        assertEquals("$esc[B", TerminalKeys.sequenceOf("↓"))
        assertEquals("$esc[D", TerminalKeys.sequenceOf("←"))
        assertEquals("$esc[C", TerminalKeys.sequenceOf("→"))
    }

    @Test
    fun `control keys map to control bytes`() {
        assertEquals(3.toChar().toString(), TerminalKeys.sequenceOf("Ctrl-C"))
        assertEquals(4.toChar().toString(), TerminalKeys.sequenceOf("Ctrl-D"))
        assertEquals(26.toChar().toString(), TerminalKeys.sequenceOf("Ctrl-Z"))
        assertEquals(12.toChar().toString(), TerminalKeys.sequenceOf("Ctrl-L"))
    }

    @Test
    fun `esc and tab and paging`() {
        assertEquals(esc.toString(), TerminalKeys.sequenceOf("Esc"))
        assertEquals("\t", TerminalKeys.sequenceOf("Tab"))
        assertEquals("$esc[5~", TerminalKeys.sequenceOf("PgUp"))
        assertEquals("$esc[6~", TerminalKeys.sequenceOf("PgDn"))
        assertEquals("$esc[3~", TerminalKeys.sequenceOf("Del"))
    }

    @Test
    fun `unknown key returns null and displayOrder covers all`() {
        assertNull(TerminalKeys.sequenceOf("F13"))
        assertEquals(TerminalKeys.sequences.size, TerminalKeys.displayOrder.size)
        assertTrue(TerminalKeys.displayOrder.contains("↑"))
    }

    @Test
    fun `geometry computes cols and rows from pixels`() {
        // 800px / 10px = 80 cols, 1200px / 20px = 60 rows
        assertEquals(80 to 60, TerminalGeometry.compute(800f, 1200f, 10f, 20f))
    }

    @Test
    fun `geometry clamps to minimums`() {
        val (cols, rows) = TerminalGeometry.compute(50f, 30f, 10f, 20f)
        assertEquals(TerminalGeometry.MIN_COLS, cols)
        assertEquals(TerminalGeometry.MIN_ROWS, rows)
    }

    @Test
    fun `geometry falls back on zero char size`() {
        assertEquals(80 to 24, TerminalGeometry.compute(800f, 1200f, 0f, 20f))
        assertEquals(80 to 24, TerminalGeometry.compute(0f, 0f, 10f, 20f))
    }
}
