package com.nuclearboy.api.deepseek

fun sanitizeProviderModelName(raw: String): String =
    raw.filterNot { it.isInvisibleProviderTextChar() }.trim()

fun sanitizeProviderBaseUrl(raw: String): String =
    raw.filterNot { it.isInvisibleProviderTextChar() }.trim()

private fun Char.isInvisibleProviderTextChar(): Boolean {
    val type = Character.getType(this)
    return type == Character.FORMAT.toInt() || type == Character.CONTROL.toInt()
}
