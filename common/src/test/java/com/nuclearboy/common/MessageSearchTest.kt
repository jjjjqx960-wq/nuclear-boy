package com.nuclearboy.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageSearchTest {

    private fun m(role: MessageRole, content: String) = ChatMessage(role = role, content = content)

    private val convo = listOf(
        m(MessageRole.USER, "帮我修复编译错误"),
        m(MessageRole.ASSISTANT, "好的，编译已通过 ✨"),
        m(MessageRole.SYSTEM, "系统：编译环境就绪"),
        m(MessageRole.USER, "再写个测试"),
    )

    @Test
    fun `finds matches by substring across user and assistant`() {
        assertEquals(listOf(0, 1), MessageSearch.find(convo, "编译"))
    }

    @Test
    fun `case insensitive`() {
        val c = listOf(m(MessageRole.ASSISTANT, "Build SUCCESSFUL"))
        assertEquals(listOf(0), MessageSearch.find(c, "successful"))
    }

    @Test
    fun `skips system messages`() {
        // "编译环境就绪" 在系统消息里，但系统消息不计入
        val hits = MessageSearch.find(convo, "环境")
        assertEquals(emptyList<Int>(), hits)
    }

    @Test
    fun `blank query returns empty`() {
        assertEquals(emptyList<Int>(), MessageSearch.find(convo, "   "))
        assertEquals(emptyList<Int>(), MessageSearch.find(convo, ""))
    }

    @Test
    fun `no match returns empty`() {
        assertEquals(emptyList<Int>(), MessageSearch.find(convo, "不存在的词"))
    }

    @Test
    fun `count matches size`() {
        assertEquals(2, MessageSearch.count(convo, "编译"))
        assertEquals(1, MessageSearch.count(convo, "测试"))
    }
}
