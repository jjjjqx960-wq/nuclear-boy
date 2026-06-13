package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalEmulatorTest {

    private val esc = 27.toChar()

    private fun line(e: TerminalEmulator, row: Int): String =
        e.renderText().split("\n").getOrElse(row) { "" }

    @Test
    fun `writes plain text on first line`() {
        val e = TerminalEmulator(20, 5)
        e.feed("hello")
        assertEquals("hello", line(e, 0))
    }

    @Test
    fun `newline moves to next row and carriage return resets column`() {
        val e = TerminalEmulator(20, 5)
        e.feed("ab\r\ncd")
        assertEquals("ab", line(e, 0))
        assertEquals("cd", line(e, 1))
    }

    @Test
    fun `cursor position CUP writes at absolute location`() {
        val e = TerminalEmulator(20, 5)
        e.feed("${esc}[2;3HX")  // row2 col3 (1-based)
        assertEquals("  X", line(e, 1))
    }

    @Test
    fun `erase line to end clears rightward`() {
        val e = TerminalEmulator(20, 5)
        e.feed("ABCDE")
        e.feed("${esc}[1;3H")   // back to col3
        e.feed("${esc}[0K")     // erase to end
        assertEquals("AB", line(e, 0))
    }

    @Test
    fun `erase display clears whole screen and homes cursor`() {
        val e = TerminalEmulator(20, 5)
        e.feed("line1\r\nline2")
        e.feed("${esc}[2J")
        assertEquals("", line(e, 0))
        assertEquals("", line(e, 1))
        e.feed("Z")
        assertEquals("Z", line(e, 0)) // 光标已回到左上
    }

    @Test
    fun `overwrites in place via cursor addressing not appending`() {
        val e = TerminalEmulator(20, 3)
        e.feed("hello world")
        e.feed("${esc}[1;1H")
        e.feed("HELLO")
        assertEquals("HELLO world", line(e, 0))
    }

    @Test
    fun `scrolls when writing past bottom row`() {
        val e = TerminalEmulator(10, 2)
        e.feed("a\r\nb\r\nc")  // 3 行进 2 行屏幕 → 第一行滚走
        assertEquals("b", line(e, 0))
        assertEquals("c", line(e, 1))
    }

    @Test
    fun `SGR color applies to rendered span`() {
        val e = TerminalEmulator(20, 2)
        e.feed("${esc}[31mERR${esc}[0m")
        val spans = e.render()[0]
        assertEquals("ERR", spans[0].text)
        assertEquals(0xFFCD5C5C.toInt(), spans[0].fgArgb)
    }

    @Test
    fun `alt screen buffer isolates then restores main`() {
        val e = TerminalEmulator(20, 3)
        e.feed("main content")
        e.feed("${esc}[?1049h")        // 进入备用屏
        e.feed("${esc}[2JTUI screen")  // 备用屏清屏后写
        assertEquals("TUI screen", line(e, 0))
        e.feed("${esc}[?1049l")        // 退出备用屏 → 恢复主屏
        assertEquals("main content", line(e, 0))
    }

    @Test
    fun `cursor up and column moves`() {
        val e = TerminalEmulator(20, 5)
        e.feed("${esc}[3;5H")  // row3 col5
        e.feed("${esc}[A")     // up one
        e.feed("X")
        assertEquals("    X", line(e, 1)) // row2 (index1), col5(index4)
    }

    @Test
    fun `resize preserves top-left content`() {
        val e = TerminalEmulator(20, 5)
        e.feed("keep")
        e.resize(40, 10)
        assertEquals("keep", line(e, 0))
        assertEquals(40, e.cols)
        assertEquals(10, e.rows)
    }

    @Test
    fun `line wrap advances to next row`() {
        val e = TerminalEmulator(3, 3)
        e.feed("abcd")  // 3 列，第 4 个字符换行
        assertEquals("abc", line(e, 0))
        assertEquals("d", line(e, 1))
    }

    @Test
    fun `scrollback keeps lines scrolled off the top in normal buffer`() {
        val e = TerminalEmulator(10, 2)
        e.feed("a\r\nb\r\nc")  // "a" 滚出屏幕
        // 可见屏只剩 b/c
        assertEquals("b", line(e, 0))
        // 完整渲染（含回滚）应保留 a
        val full = e.renderWithScrollback().map { spans -> spans.joinToString("") { it.text } }
        assertEquals("a", full.first())
        assertTrue(full.contains("c"))
    }

    @Test
    fun `alt buffer render excludes scrollback`() {
        val e = TerminalEmulator(10, 2)
        e.feed("x\r\ny\r\nz")          // 产生回滚
        e.feed("${esc}[?1049h${esc}[2JT") // 进入备用屏
        val full = e.renderWithScrollback().map { spans -> spans.joinToString("") { it.text } }
        assertTrue(full.none { it == "x" }) // 备用屏不显示主屏回滚
        assertEquals("T", full.first())
    }

    @Test
    fun `cjk chars advance cursor by two cells keeping alignment`() {
        val e = TerminalEmulator(20, 3)
        val zh = "${0x4E2D.toChar()}${0x6587.toChar()}" // "中文"
        e.feed(zh)
        e.feed("X")
        // 中(2) + 文(2) = 第 5 列(index4) 才是 X
        val text = e.renderText().split("\n")[0]
        assertEquals(zh + "X", text)
        // 用 CUP 定位到第 5 列写 Y，应正好落在 X 处（说明宽字符占了 4 格）
        e.feed("${esc}[1;5HY")
        assertEquals(zh + "Y", e.renderText().split("\n")[0])
    }

    @Test
    fun `cjk wraps when only one cell remains`() {
        val e = TerminalEmulator(3, 3)  // 3 列
        e.feed("a${0x4E2D.toChar()}")    // a 占 1 列，中 需要 2 列但只剩 2 列(col1,2) 放得下
        assertEquals("a${0x4E2D.toChar()}", e.renderText().split("\n")[0])
        val e2 = TerminalEmulator(3, 3)
        e2.feed("ab${0x4E2D.toChar()}")  // ab 占 2 列，只剩 1 列 → 中 换行
        assertEquals("ab", e2.renderText().split("\n")[0])
        assertEquals("${0x4E2D.toChar()}", e2.renderText().split("\n")[1])
    }

    @Test
    fun `resize during alt buffer preserves main screen for restore`() {
        val e = TerminalEmulator(20, 3)
        e.feed("main content")
        e.feed("${esc}[?1049h${esc}[2JTUI")  // 进备用屏
        e.resize(40, 6)                       // 在 TUI 里改大小
        e.feed("${esc}[?1049l")               // 退出 → 应还原主屏
        assertEquals("main content", line(e, 0))
    }

    @Test
    fun `narrow char overwriting wide char clears its continuation`() {
        val e = TerminalEmulator(20, 2)
        e.feed("${0x4E2D.toChar()}${0x6587.toChar()}") // "中文"
        e.feed("${esc}[1;1HAB")  // 用 AB 覆盖"中"(占2格)
        // 不应残留续格导致错位：第一行应是 "AB文"
        assertEquals("AB${0x6587.toChar()}", e.renderText().split("\n")[0])
    }

    @Test
    fun `ignores OSC title sequence`() {
        val e = TerminalEmulator(20, 2)
        e.feed("${esc}]0;my title${7.toChar()}done")
        assertEquals("done", line(e, 0))
    }
}
