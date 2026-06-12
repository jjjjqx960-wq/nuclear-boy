package com.nuclearboy.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatEditingTest {

    private fun m(id: String, role: MessageRole, content: String) =
        ChatMessage(id = id, role = role, content = content)

    private val convo = listOf(
        m("u1", MessageRole.USER, "第一个问题"),
        m("a1", MessageRole.ASSISTANT, "第一个回答"),
        m("u2", MessageRole.USER, "第二个问题"),
        m("a2", MessageRole.ASSISTANT, "第二个回答"),
    )

    @Test
    fun `prepareEdit returns content and truncates from that message`() {
        val r = ChatEditing.prepareEdit(convo, "u2")!!
        assertEquals("第二个问题", r.content)
        // 截断后只剩 u1、a1
        assertEquals(listOf("u1", "a1"), r.remaining.map { it.id })
    }

    @Test
    fun `editing first message clears whole conversation`() {
        val r = ChatEditing.prepareEdit(convo, "u1")!!
        assertEquals("第一个问题", r.content)
        assertEquals(emptyList<String>(), r.remaining.map { it.id })
    }

    @Test
    fun `non-user message returns null`() {
        assertNull(ChatEditing.prepareEdit(convo, "a1"))
    }

    @Test
    fun `unknown id returns null`() {
        assertNull(ChatEditing.prepareEdit(convo, "nope"))
    }

    @Test
    fun `does not mutate original list`() {
        ChatEditing.prepareEdit(convo, "u2")
        assertEquals(4, convo.size)
    }

    @Test
    fun `removeMessage drops only the target`() {
        val r = ChatEditing.removeMessage(convo, "a1")
        assertEquals(listOf("u1", "u2", "a2"), r.map { it.id })
    }

    @Test
    fun `removeMessage unknown id keeps list`() {
        assertEquals(4, ChatEditing.removeMessage(convo, "nope").size)
    }
}
