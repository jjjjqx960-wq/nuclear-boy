package com.nuclearboy.remotepc

/**
 * 终端单元格宽度（借鉴 wcwidth / East Asian Width）。
 *
 * CJK（中文/日文/한글）及全角符号在等宽终端里占 2 列，ASCII 占 1 列。终端网格
 * 模拟器必须据此推进光标，否则中文输出会错位——对核弹男孩这种中文产品尤其关键。
 * 仅覆盖 BMP 常见宽字符范围；组合记号按 0 宽处理；补充平面 CJK（代理对）暂按 1。
 */
object CharWidth {

    /** 返回字符显示宽度：宽字符=2，组合记号=0，其余=1。 */
    fun of(ch: Char): Int {
        val c = ch.code
        if (c == 0) return 0
        if (isCombining(c)) return 0
        if (isWide(c)) return 2
        return 1
    }

    private fun isWide(c: Int): Boolean = when (c) {
        in 0x1100..0x115F -> true   // Hangul Jamo
        0x2329, 0x232A -> true      // 角括号
        in 0x2E80..0x303E -> true   // CJK 部首、康熙部首、CJK 符号
        in 0x3041..0x33FF -> true   // 平假名/片假名/注音/CJK 符号方块
        in 0x3400..0x4DBF -> true   // CJK 扩展 A
        in 0x4E00..0x9FFF -> true   // CJK 统一表意
        in 0xA000..0xA4CF -> true   // 彝文
        in 0xAC00..0xD7A3 -> true   // 谚文音节
        in 0xF900..0xFAFF -> true   // CJK 兼容表意
        in 0xFE10..0xFE19 -> true   // 竖排标点
        in 0xFE30..0xFE6F -> true   // CJK 兼容形式、小写变体
        in 0xFF00..0xFF60 -> true   // 全角 ASCII 变体
        in 0xFFE0..0xFFE6 -> true   // 全角符号
        else -> false
    }

    private fun isCombining(c: Int): Boolean = when (c) {
        in 0x0300..0x036F -> true   // 组合附加符号
        in 0x1AB0..0x1AFF -> true
        in 0x1DC0..0x1DFF -> true
        in 0x20D0..0x20FF -> true   // 组合记号符号
        in 0xFE20..0xFE2F -> true   // 组合半符号
        else -> false
    }
}
