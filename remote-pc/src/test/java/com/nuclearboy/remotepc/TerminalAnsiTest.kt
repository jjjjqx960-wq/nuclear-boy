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

    // ── parseSpans（颜色渲染）──────────────────────────────

    @Test
    fun `parseSpans splits colored segments and preserves text`() {
        val spans = TerminalAnsi.parseSpans("${esc}[31mERR${esc}[0m ok")
        // 拼回的纯文本应与 strip 一致
        assertEquals("ERR ok", spans.joinToString("") { it.text })
        // 第一段是红色，复位后默认色
        assertEquals("ERR", spans[0].text)
        assertEquals(0xFFCD5C5C.toInt(), spans[0].fgArgb)
        assertEquals(null, spans.last().fgArgb)
    }

    @Test
    fun `parseSpans handles bold and bright color`() {
        val spans = TerminalAnsi.parseSpans("${esc}[1;92mY")
        assertEquals(true, spans[0].bold)
        assertEquals(0xFF87FF87.toInt(), spans[0].fgArgb) // bright green (palette[10])
    }

    @Test
    fun `parseSpans handles truecolor`() {
        val spans = TerminalAnsi.parseSpans("${esc}[38;2;10;20;30mX")
        assertEquals(0xFF0A141E.toInt(), spans[0].fgArgb)
    }

    @Test
    fun `parseSpans handles 256 color`() {
        val spans = TerminalAnsi.parseSpans("${esc}[38;5;196mX")
        // 196 在 6x6x6 cube: c=180 -> r=5,g=0,b=0 -> (255,0,0)
        assertEquals(0xFFFF0000.toInt(), spans[0].fgArgb)
    }

    @Test
    fun `parseSpans ignores background and cursor sequences`() {
        val spans = TerminalAnsi.parseSpans("${esc}[44mA${esc}[2KB")
        assertEquals("AB", spans.joinToString("") { it.text })
        // 背景色被忽略，前景保持默认
        assertEquals(null, spans[0].fgArgb)
    }

    @Test
    fun `parseSpans plain text is single default span`() {
        val spans = TerminalAnsi.parseSpans("hello")
        assertEquals(1, spans.size)
        assertEquals("hello", spans[0].text)
        assertEquals(null, spans[0].fgArgb)
    }
}
