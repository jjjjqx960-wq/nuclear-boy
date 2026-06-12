package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalAnsiTest {

    private val esc = 27.toChar()
    private val bel = 7.toChar()

    @Test
    fun `strips CSI color codes keeps text`() {
        val input = "${esc}[32mhello${esc}[0m world"
        assertEquals("hello world", TerminalAnsi.strip(input))
    }

    @Test
    fun `strips cursor positioning CSI`() {
        val input = "A${esc}[4;9HB${esc}[2KC"
        assertEquals("ABC", TerminalAnsi.strip(input))
    }

    @Test
    fun `strips OSC title sequence terminated by BEL`() {
        val input = "${esc}]0;window title${bel}prompt>"
        assertEquals("prompt>", TerminalAnsi.strip(input))
    }

    @Test
    fun `strips OSC terminated by ST`() {
        val input = "${esc}]0;t${esc}\\done"
        assertEquals("done", TerminalAnsi.strip(input))
    }

    @Test
    fun `backspace deletes previous visible char`() {
        assertEquals("ac", TerminalAnsi.strip("ab\bc"))
    }

    @Test
    fun `keeps newlines drops bare carriage returns and bells`() {
        assertEquals("line1\nline2", TerminalAnsi.strip("line1\r\nline2$bel"))
    }

    @Test
    fun `plain text unchanged`() {
        assertEquals("just text 123", TerminalAnsi.strip("just text 123"))
    }
}
