package com.nuclearboy.agent

import com.nuclearboy.api.deepseek.FunctionCallDto
import com.nuclearboy.api.deepseek.ToolCallDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallLoopGuardTest {

    @Test
    fun `first tool batch does not stop`() {
        val guard = ToolCallLoopGuard()

        val observation = guard.observe(listOf(toolCall("call-1", "read_file", """{"path":"a"}""")))

        assertEquals(1, observation.consecutiveDuplicateBatches)
        assertFalse(observation.shouldStop)
    }

    @Test
    fun `third exact duplicate batch stops loop`() {
        val guard = ToolCallLoopGuard()
        val calls = listOf(toolCall("call-1", "read_file", """{"path":"a"}"""))

        assertFalse(guard.observe(calls).shouldStop)
        assertFalse(guard.observe(calls).shouldStop)
        val third = guard.observe(calls)

        assertEquals(3, third.consecutiveDuplicateBatches)
        assertTrue(third.shouldStop)
    }

    @Test
    fun `different arguments reset duplicate count`() {
        val guard = ToolCallLoopGuard()

        guard.observe(listOf(toolCall("call-1", "read_file", """{"path":"a"}""")))
        guard.observe(listOf(toolCall("call-2", "read_file", """{"path":"a"}""")))
        val changed = guard.observe(listOf(toolCall("call-3", "read_file", """{"path":"b"}""")))

        assertEquals(1, changed.consecutiveDuplicateBatches)
        assertFalse(changed.shouldStop)
    }

    @Test
    fun `tool ids are ignored when detecting repeated work`() {
        val guard = ToolCallLoopGuard()

        guard.observe(listOf(toolCall("call-1", "run_python", """{"script":"print(1)"}""")))
        val second = guard.observe(listOf(toolCall("call-2", "run_python", """{"script":"print(1)"}""")))

        assertEquals(2, second.consecutiveDuplicateBatches)
        assertFalse(second.shouldStop)
    }

    private fun toolCall(id: String, name: String, arguments: String): ToolCallDto =
        ToolCallDto(
            id = id,
            function = FunctionCallDto(
                name = name,
                arguments = arguments,
            ),
        )
}
