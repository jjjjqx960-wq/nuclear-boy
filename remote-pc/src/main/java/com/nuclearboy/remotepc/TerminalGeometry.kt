package com.nuclearboy.remotepc

/**
 * 由可视区域像素尺寸与等宽字符尺寸算出终端的列数/行数，
 * 让电脑端 ConPTY 按手机实际屏幕排版（而不是固定 80×24 导致溢出横滚）。
 */
object TerminalGeometry {

    const val MIN_COLS = 20
    const val MIN_ROWS = 6
    const val MAX_COLS = 400
    const val MAX_ROWS = 200

    /** 像素 → (cols, rows)，带上下限钳制。字符尺寸非正时回退到 80×24。 */
    fun compute(
        widthPx: Float,
        heightPx: Float,
        charWidthPx: Float,
        charHeightPx: Float,
    ): Pair<Int, Int> {
        if (charWidthPx <= 0f || charHeightPx <= 0f || widthPx <= 0f || heightPx <= 0f) {
            return 80 to 24
        }
        val cols = (widthPx / charWidthPx).toInt().coerceIn(MIN_COLS, MAX_COLS)
        val rows = (heightPx / charHeightPx).toInt().coerceIn(MIN_ROWS, MAX_ROWS)
        return cols to rows
    }
}
