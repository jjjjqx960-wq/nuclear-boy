package com.nuclearboy.remotepc

/**
 * 终端输出清洗：ConPTY 吐出的是带 ANSI 转义序列的原始流，MVP 阶段不做完整
 * xterm 渲染，先把控制序列剥掉得到可读纯文本（颜色/光标定位等先忽略）。
 *
 * 处理：CSI（ESC [ ... 字母）、OSC（ESC ] ... BEL/ST）、单字符 ESC 序列、
 * 退格、回车光标回退；保留换行和可见文本。
 */
object TerminalAnsi {

    private val ESC = 27.toChar()
    private val BEL = 7.toChar()

    /** 一段同样式的文本。fgArgb 为 null 表示默认前景色；颜色用 ARGB Int（remote-pc 不依赖 Compose）。 */
    data class Span(val text: String, val fgArgb: Int? = null, val bold: Boolean = false)

    // 标准 xterm 16 色调色板（深色背景下可读），index 0-7 普通、8-15 高亮
    private val PALETTE_16 = intArrayOf(
        0xFF1E1E1E.toInt(), 0xFFCD5C5C.toInt(), 0xFF5FAF5F.toInt(), 0xFFCDCD5C.toInt(),
        0xFF5C7FCD.toInt(), 0xFFCD5CCD.toInt(), 0xFF5CCDCD.toInt(), 0xFFE5E5E5.toInt(),
        0xFF7F7F7F.toInt(), 0xFFFF6E6E.toInt(), 0xFF87FF87.toInt(), 0xFFFFFF87.toInt(),
        0xFF8FAFFF.toInt(), 0xFFFF87FF.toInt(), 0xFF87FFFF.toInt(), 0xFFFFFFFF.toInt(),
    )

    fun strip(input: String): String {
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == ESC && i + 1 < input.length -> {
                    when (input[i + 1]) {
                        '[' -> {
                            // CSI: ESC [ 参数字节(0x30-0x3F)* 中间字节(0x20-0x2F)* 终止字节(0x40-0x7E)
                            i += 2
                            while (i < input.length && input[i] in ' '..'?') i++
                            if (i < input.length) i++ // 终止字节
                        }
                        ']' -> {
                            // OSC: ESC ] ... (BEL 或 ESC \ 结束)
                            i += 2
                            while (i < input.length && input[i] != BEL &&
                                !(input[i] == ESC && i + 1 < input.length && input[i + 1] == '\\')
                            ) i++
                            if (i < input.length && input[i] == ESC) i++ // 跳过 ST 的 ESC
                            if (i < input.length) i++ // 跳过 BEL 或 \
                        }
                        else -> i += 2 // 其它两字符 ESC 序列
                    }
                }
                c == BEL -> i++             // 响铃，丢弃
                c == '\b' -> {             // 退格：删掉前一个可见字符
                    if (out.isNotEmpty() && out.last() != '\n') out.deleteCharAt(out.length - 1)
                    i++
                }
                c == '\r' -> i++           // 回车单独出现时忽略（换行用 \n）
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    /**
     * 解析成带颜色的 span 列表（借鉴 VT-100/xterm SGR）：支持 30-37/90-97 前景、
     * 38;5;n 256 色、38;2;r;g;b 真彩、加粗(1)、复位(0)；背景色与光标/OSC 序列被忽略
     * （保持深色背景）。其余清洗同 strip。完整全屏 TUI（光标定位重排）仍未实现。
     */
    fun parseSpans(input: String): List<Span> {
        val spans = ArrayList<Span>()
        val run = StringBuilder()
        var curFg: Int? = null
        var curBold = false

        fun flush() {
            if (run.isNotEmpty()) {
                spans.add(Span(run.toString(), curFg, curBold))
                run.setLength(0)
            }
        }

        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == ESC && i + 1 < input.length -> {
                    when (input[i + 1]) {
                        '[' -> {
                            var j = i + 2
                            while (j < input.length && input[j] in ' '..'?') j++
                            val params = input.substring(i + 2, j)
                            val final = if (j < input.length) input[j] else ' '
                            if (final == 'm') {
                                flush()
                                val (fg, bold) = applySgr(params, curFg, curBold)
                                curFg = fg; curBold = bold
                            }
                            i = if (j < input.length) j + 1 else j
                        }
                        ']' -> {
                            i += 2
                            while (i < input.length && input[i] != BEL &&
                                !(input[i] == ESC && i + 1 < input.length && input[i + 1] == '\\')
                            ) i++
                            if (i < input.length && input[i] == ESC) i++
                            if (i < input.length) i++
                        }
                        else -> i += 2
                    }
                }
                c == BEL -> i++
                c == '\b' -> {
                    if (run.isNotEmpty() && run.last() != '\n') run.deleteCharAt(run.length - 1)
                    i++
                }
                c == '\r' -> i++
                else -> {
                    run.append(c)
                    i++
                }
            }
        }
        flush()
        return spans
    }

    /** 处理 SGR 参数串，返回新的 (前景色, 加粗)。internal 供 TerminalEmulator 复用。 */
    internal fun applySgr(params: String, fg0: Int?, bold0: Boolean): Pair<Int?, Boolean> {
        if (params.isEmpty()) return Pair(null, false) // ESC[m == ESC[0m 复位
        val codes = params.split(';').map { it.toIntOrNull() ?: 0 }
        var fg = fg0
        var bold = bold0
        var k = 0
        while (k < codes.size) {
            when (val code = codes[k]) {
                0 -> { fg = null; bold = false }
                1 -> bold = true
                22 -> bold = false
                in 30..37 -> fg = PALETTE_16[code - 30]
                39 -> fg = null
                in 90..97 -> fg = PALETTE_16[8 + (code - 90)]
                38 -> {
                    // 38;5;n 或 38;2;r;g;b
                    if (k + 1 < codes.size && codes[k + 1] == 5 && k + 2 < codes.size) {
                        fg = xterm256(codes[k + 2]); k += 2
                    } else if (k + 1 < codes.size && codes[k + 1] == 2 && k + 4 < codes.size) {
                        fg = (0xFF shl 24) or (codes[k + 2] shl 16) or (codes[k + 3] shl 8) or codes[k + 4]
                        k += 4
                    }
                }
                // 背景色 40-47/100-107/48 及 49 忽略（保持深色背景），但需跳过 48 的扩展参数
                48 -> {
                    if (k + 1 < codes.size && codes[k + 1] == 5) k += 2
                    else if (k + 1 < codes.size && codes[k + 1] == 2) k += 4
                }
                else -> Unit
            }
            k++
        }
        return Pair(fg, bold)
    }

    /** xterm 256 色编号转 ARGB。 */
    private fun xterm256(n: Int): Int {
        val idx = n.coerceIn(0, 255)
        if (idx < 16) return PALETTE_16[idx]
        if (idx in 16..231) {
            val c = idx - 16
            val r = c / 36; val g = (c % 36) / 6; val b = c % 6
            fun lvl(v: Int) = if (v == 0) 0 else 55 + v * 40
            return (0xFF shl 24) or (lvl(r) shl 16) or (lvl(g) shl 8) or lvl(b)
        }
        val gray = 8 + (idx - 232) * 10
        return (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
}
