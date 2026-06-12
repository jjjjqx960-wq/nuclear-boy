package com.nuclearboy.common

/**
 * Token 用量的简短展示（借鉴主流 AI 客户端在每条回复下显示 ↑输入/↓输出）。
 *
 * 之前只显示总量，看不出输入/输出占比，也看不到缓存命中（DeepSeek 命中缓存大幅省钱）。
 * 纯函数、无 Android 依赖，便于单测。
 */
object TokenUsageFormat {

    private fun short(count: Long): String =
        if (count >= 1000) "%.1fk".format(count / 1000.0) else count.toString()

    /**
     * 形如 "↑1.2k ↓456"（有缓存命中再追加 " ·缓存800"）。两者皆 0 时返回总量回退。
     */
    fun inline(usage: TokenUsage): String {
        val parts = StringBuilder()
        if (usage.promptTokens > 0) parts.append("↑").append(short(usage.promptTokens))
        if (usage.completionTokens > 0) {
            if (parts.isNotEmpty()) parts.append(" ")
            parts.append("↓").append(short(usage.completionTokens))
        }
        if (parts.isEmpty()) {
            // 没有拆分数据时回退到总量
            return short(usage.totalTokens) + " tokens"
        }
        if (usage.cachedPromptTokens > 0) {
            parts.append(" ·缓存").append(short(usage.cachedPromptTokens))
        }
        return parts.toString()
    }
}
