package com.nuclearboy.common

/**
 * 会话内搜索的纯逻辑：在消息内容里找匹配，返回命中的消息下标（用于滚动定位）。
 *
 * 大小写不敏感，跳过系统消息与空查询。纯函数、无 Android 依赖，便于单测；
 * UI 层据返回的下标滚动到对应消息并高亮当前命中。
 */
object MessageSearch {

    /** 返回内容包含 query 的消息在列表中的下标，按出现顺序。query 空白时返回空。 */
    fun find(messages: List<ChatMessage>, query: String): List<Int> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val needle = q.lowercase()
        val hits = ArrayList<Int>()
        for (i in messages.indices) {
            val m = messages[i]
            if (m.role == MessageRole.SYSTEM) continue
            if (m.content.lowercase().contains(needle)) hits.add(i)
        }
        return hits
    }

    /** 命中数量。 */
    fun count(messages: List<ChatMessage>, query: String): Int = find(messages, query).size
}
