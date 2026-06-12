package com.nuclearboy.remotepc

/**
 * 终端特殊键 → 转义序列映射（借鉴 ReTerminal VirtualKeyClient）。
 *
 * 全屏 TUI（vim、菜单、claude 交互界面）必须有方向键等；普通输入栏只能打可见
 * 字符，这里补齐方向/翻页/Home-End/常用 Ctrl 组合的标准 xterm 序列。
 */
object TerminalKeys {

    private val ESC = 27.toChar().toString()
    private fun ctrl(c: Char) = (c.code - 64).toChar().toString() // Ctrl-A=1 ... Ctrl-Z=26

    /** 键名 → 要写入终端的字节序列。 */
    val sequences: Map<String, String> = linkedMapOf(
        "↑" to "$ESC[A",
        "↓" to "$ESC[B",
        "←" to "$ESC[D",
        "→" to "$ESC[C",
        "Esc" to ESC,
        "Tab" to "\t",
        "Ctrl-C" to ctrl('C'),
        "Ctrl-D" to ctrl('D'),
        "Ctrl-Z" to ctrl('Z'),
        "Ctrl-L" to ctrl('L'),
        "Home" to "$ESC[H",
        "End" to "$ESC[F",
        "PgUp" to "$ESC[5~",
        "PgDn" to "$ESC[6~",
        "Del" to "$ESC[3~",
    )

    /** 取某个键的序列；未知键返回 null。 */
    fun sequenceOf(name: String): String? = sequences[name]

    /** 界面快捷键栏展示顺序。 */
    val displayOrder: List<String> = sequences.keys.toList()
}
