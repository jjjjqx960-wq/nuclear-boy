package com.nuclearboy.api.deepseek

internal fun sanitizeProviderModelName(raw: String): String =
    raw.filterNot { it.isInvisibleModelNameChar() }.trim()

private fun Char.isInvisibleModelNameChar(): Boolean {
    val type = Character.getType(this)
    return type == Character.FORMAT.toInt() || type == Character.CONTROL.toInt()
}
