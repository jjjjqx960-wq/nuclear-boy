package com.nuclearboy.common

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolProgressBusTest {

    @Test
    fun `report delivers progress to collector`() = runTest {
        val received = mutableListOf<ToolProgressBus.ToolProgress>()
        val job = launch {
            received.add(ToolProgressBus.events.first())
        }
        // 等订阅者就绪后上报
        testScheduler.runCurrent()
        ToolProgressBus.report("pc_cli_run", "[工具] Bash")
        job.join()

        assertEquals(1, received.size)
        assertEquals("pc_cli_run", received[0].toolName)
        assertEquals("[工具] Bash", received[0].text)
    }

    @Test
    fun `report truncates overlong line`() = runTest {
        val job = launch {
            val progress = ToolProgressBus.events.first()
            assertTrue(progress.text.length <= 300)
        }
        testScheduler.runCurrent()
        ToolProgressBus.report("pc_cli_run", "x".repeat(5000))
        job.join()
    }

    @Test
    fun `blank text is dropped without subscriber error`() {
        // 无订阅者 + 空文本：不抛异常即通过
        ToolProgressBus.report("pc_cli_run", "   ")
        ToolProgressBus.report("pc_cli_run", "")
    }
}
