package com.nuclearboy.remotepc

/**
 * 轻量 VT100/xterm 屏幕缓冲终端模拟器（借鉴 VtNetCore/termux 的网格模型）。
 *
 * 维护 rows×cols 的字符网格 + 光标，处理光标定位、擦除、滚动区、SGR 颜色、
 * 备用屏幕缓冲（alt screen，vim/htop/claude TUI 用）——让全屏 TUI 正确排版，
 * 而不只是逐行追加。不实现：双宽字符、字符集切换、鼠标上报等高级特性。
 */
class TerminalEmulator(cols: Int = 80, rows: Int = 24) {

    data class Cell(val ch: Char = ' ', val fg: Int? = null, val bold: Boolean = false)

    var cols = cols.coerceAtLeast(1); private set
    var rows = rows.coerceAtLeast(1); private set

    private var grid = newGrid(this.rows, this.cols)
    private var altGrid: Array<Array<Cell>>? = null
    // 普通缓冲的回滚历史（滚出顶部的行）；备用屏（TUI 全屏）不计入
    private val scrollback = ArrayDeque<Array<Cell>>()

    private var row = 0
    private var col = 0
    private var savedRow = 0
    private var savedCol = 0
    private var curFg: Int? = null
    private var curBold = false
    private var scrollTop = 0
    private var scrollBottom = this.rows - 1

    private val esc = 27.toChar()
    private val bel = 7.toChar()

    private companion object {
        const val MAX_SCROLLBACK = 2000
    }

    private fun newGrid(r: Int, c: Int) = Array(r) { Array(c) { Cell() } }

    /** 喂入终端输出流（含 ANSI），更新网格。 */
    fun feed(input: String) {
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                ch == esc && i + 1 < input.length -> i = handleEscape(input, i)
                ch == '\n' -> { lineFeed(); i++ }
                ch == '\r' -> { col = 0; i++ }
                ch == '\b' -> { if (col > 0) col--; i++ }
                ch == '\t' -> { col = ((col / 8) + 1) * 8; if (col >= cols) col = cols - 1; i++ }
                ch == bel -> i++
                ch.code < 32 -> i++ // 其它控制字符丢弃
                else -> { putChar(ch); i++ }
            }
        }
    }

    private fun handleEscape(s: String, start: Int): Int {
        val next = s[start + 1]
        return when (next) {
            '[' -> handleCsi(s, start)
            ']' -> { // OSC：跳到 BEL 或 ST
                var i = start + 2
                while (i < s.length && s[i] != bel &&
                    !(s[i] == esc && i + 1 < s.length && s[i + 1] == '\\')
                ) i++
                if (i < s.length && s[i] == esc) i++
                if (i < s.length) i++
                i
            }
            '7' -> { savedRow = row; savedCol = col; start + 2 }   // DECSC
            '8' -> { row = savedRow; col = savedCol; start + 2 }   // DECRC
            'M' -> { reverseLineFeed(); start + 2 }                // RI
            else -> start + 2
        }
    }

    private fun handleCsi(s: String, start: Int): Int {
        var j = start + 2
        val priv = j < s.length && s[j] == '?'
        if (priv) j++
        val paramStart = j
        while (j < s.length && (s[j] in '0'..'9' || s[j] == ';')) j++
        val params = s.substring(paramStart, j)
        if (j >= s.length) return j
        val final = s[j]
        val nums = params.split(';').map { it.toIntOrNull() }
        fun p(idx: Int, def: Int) = nums.getOrNull(idx) ?: def

        if (priv) {
            handlePrivateMode(params, final)
            return j + 1
        }

        when (final) {
            'H', 'f' -> { row = (p(0, 1) - 1).coerceIn(0, rows - 1); col = (p(1, 1) - 1).coerceIn(0, cols - 1) }
            'A' -> row = (row - p(0, 1)).coerceAtLeast(0)
            'B' -> row = (row + p(0, 1)).coerceAtMost(rows - 1)
            'C' -> col = (col + p(0, 1)).coerceAtMost(cols - 1)
            'D' -> col = (col - p(0, 1)).coerceAtLeast(0)
            'E' -> { row = (row + p(0, 1)).coerceAtMost(rows - 1); col = 0 }
            'F' -> { row = (row - p(0, 1)).coerceAtLeast(0); col = 0 }
            'G', '`' -> col = (p(0, 1) - 1).coerceIn(0, cols - 1)
            'd' -> row = (p(0, 1) - 1).coerceIn(0, rows - 1)
            'J' -> eraseInDisplay(p(0, 0))
            'K' -> eraseInLine(p(0, 0))
            'L' -> insertLines(p(0, 1))
            'M' -> deleteLines(p(0, 1))
            'P' -> deleteChars(p(0, 1))
            '@' -> insertChars(p(0, 1))
            'X' -> eraseChars(p(0, 1))
            'S' -> repeat(p(0, 1)) { scrollUp() }
            'T' -> repeat(p(0, 1)) { scrollDown() }
            'r' -> { scrollTop = (p(0, 1) - 1).coerceIn(0, rows - 1); scrollBottom = (p(1, rows) - 1).coerceIn(0, rows - 1); row = 0; col = 0 }
            's' -> { savedRow = row; savedCol = col }
            'u' -> { row = savedRow; col = savedCol }
            'm' -> { val (fg, b) = TerminalAnsi.applySgr(params, curFg, curBold); curFg = fg; curBold = b }
            else -> Unit
        }
        return j + 1
    }

    private fun handlePrivateMode(params: String, final: Char) {
        val set = final == 'h'
        val code = params.toIntOrNull()
        when (code) {
            1049, 47, 1047 -> if (set) enterAltBuffer() else exitAltBuffer()
            else -> Unit // ?25 光标显隐、?2004 bracketed paste 等忽略
        }
    }

    private fun enterAltBuffer() {
        if (altGrid == null) {
            altGrid = grid
            grid = newGrid(rows, cols)
            row = 0; col = 0
        }
    }

    private fun exitAltBuffer() {
        altGrid?.let { grid = it; altGrid = null }
    }

    private fun putChar(ch: Char) {
        if (col >= cols) { col = 0; lineFeed() }
        grid[row][col] = Cell(ch, curFg, curBold)
        col++
    }

    private fun lineFeed() {
        if (row == scrollBottom) scrollUp() else row = (row + 1).coerceAtMost(rows - 1)
    }

    private fun reverseLineFeed() {
        if (row == scrollTop) scrollDown() else row = (row - 1).coerceAtLeast(0)
    }

    private fun scrollUp() {
        // 普通缓冲、整屏滚动时把顶行存进回滚历史，保留命令输出的可追溯性
        if (altGrid == null && scrollTop == 0) {
            scrollback.addLast(grid[0])
            while (scrollback.size > MAX_SCROLLBACK) scrollback.removeFirst()
        }
        for (r in scrollTop until scrollBottom) grid[r] = grid[r + 1]
        grid[scrollBottom] = Array(cols) { Cell() }
    }

    private fun scrollDown() {
        for (r in scrollBottom downTo scrollTop + 1) grid[r] = grid[r - 1]
        grid[scrollTop] = Array(cols) { Cell() }
    }

    private fun insertLines(n: Int) {
        if (row < scrollTop || row > scrollBottom) return
        repeat(n.coerceAtMost(scrollBottom - row + 1)) {
            for (r in scrollBottom downTo row + 1) grid[r] = grid[r - 1]
            grid[row] = Array(cols) { Cell() }
        }
    }

    private fun deleteLines(n: Int) {
        if (row < scrollTop || row > scrollBottom) return
        repeat(n.coerceAtMost(scrollBottom - row + 1)) {
            for (r in row until scrollBottom) grid[r] = grid[r + 1]
            grid[scrollBottom] = Array(cols) { Cell() }
        }
    }

    private fun insertChars(n: Int) {
        val line = grid[row]
        val count = n.coerceAtMost(cols - col)
        for (c in cols - 1 downTo col + count) line[c] = line[c - count]
        for (c in col until col + count) line[c] = Cell()
    }

    private fun deleteChars(n: Int) {
        val line = grid[row]
        val count = n.coerceAtMost(cols - col)
        for (c in col until cols - count) line[c] = line[c + count]
        for (c in cols - count until cols) line[c] = Cell()
    }

    private fun eraseChars(n: Int) {
        val line = grid[row]
        for (c in col until (col + n).coerceAtMost(cols)) line[c] = Cell()
    }

    private fun eraseInLine(mode: Int) {
        val line = grid[row]
        when (mode) {
            0 -> for (c in col until cols) line[c] = Cell()
            1 -> for (c in 0..col.coerceAtMost(cols - 1)) line[c] = Cell()
            2 -> for (c in 0 until cols) line[c] = Cell()
        }
    }

    private fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> { eraseInLine(0); for (r in row + 1 until rows) grid[r] = Array(cols) { Cell() } }
            1 -> { eraseInLine(1); for (r in 0 until row) grid[r] = Array(cols) { Cell() } }
            2, 3 -> { for (r in 0 until rows) grid[r] = Array(cols) { Cell() }; row = 0; col = 0 }
        }
    }

    fun resize(newCols: Int, newRows: Int) {
        val c = newCols.coerceAtLeast(1); val r = newRows.coerceAtLeast(1)
        val ng = newGrid(r, c)
        for (rr in 0 until minOf(r, rows)) for (cc in 0 until minOf(c, cols)) ng[rr][cc] = grid[rr][cc]
        grid = ng; cols = c; rows = r
        scrollTop = 0; scrollBottom = r - 1
        row = row.coerceAtMost(r - 1); col = col.coerceAtMost(c - 1)
        altGrid = null
    }

    /** 包含回滚历史的完整渲染（普通缓冲时 = 历史 + 当前屏；备用屏时只当前屏）。 */
    fun renderWithScrollback(): List<List<TerminalAnsi.Span>> {
        val lines = if (altGrid == null && scrollback.isNotEmpty())
            (scrollback + grid.toList()) else grid.toList()
        return lines.map { renderLine(it) }
    }

    /** 渲染当前屏幕为按行的彩色 span（行尾空白裁掉，便于显示）。 */
    fun render(): List<List<TerminalAnsi.Span>> = grid.map { renderLine(it) }

    private fun renderLine(line: Array<Cell>): List<TerminalAnsi.Span> {
        val spans = ArrayList<TerminalAnsi.Span>()
        val sb = StringBuilder()
        var lastFg: Int? = null
        var lastBold = false
        var lastNonBlank = -1
        for (c in line.indices) if (line[c].ch != ' ' || line[c].fg != null) lastNonBlank = c
        val end = lastNonBlank + 1
        for (c in 0 until end) {
            val cell = line[c]
            if (sb.isNotEmpty() && (cell.fg != lastFg || cell.bold != lastBold)) {
                spans.add(TerminalAnsi.Span(sb.toString(), lastFg, lastBold)); sb.setLength(0)
            }
            if (sb.isEmpty()) { lastFg = cell.fg; lastBold = cell.bold }
            sb.append(cell.ch)
        }
        if (sb.isNotEmpty()) spans.add(TerminalAnsi.Span(sb.toString(), lastFg, lastBold))
        return spans
    }

    /** 渲染为纯文本（测试/无颜色场景）。 */
    fun renderText(): String = render().joinToString("\n") { line -> line.joinToString("") { it.text } }
}
