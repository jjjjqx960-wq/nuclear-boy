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
}
