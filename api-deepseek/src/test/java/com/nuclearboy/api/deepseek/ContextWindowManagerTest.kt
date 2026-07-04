package com.nuclearboy.api.deepseek

import com.nuclearboy.common.AppConstants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ContextWindowManagerTest {

    private lateinit var manager: ContextWindowManager

    @Before
    fun setUp() {
        manager = ContextWindowManager()
    }

    @Test
    fun `initial budget is zero`() {
        val budget = manager.budget.value
        assertEquals(0L, budget.totalUsed)
        assertEquals(AppConstants.DEEPSEEK_CONTEXT_WINDOW, budget.remaining)
        assertEquals(ContextWarningLevel.OK, budget.warningLevel)
    }

    @Test
    fun `update allocation reflects in budget`() {
        manager.updateAllocation(systemPrompt = 3000)
        assertEquals(3000, manager.budget.value.systemPrompt)
        assertEquals(3000, manager.budget.value.totalUsed)
    }

    @Test
    fun `canFit returns correct answer`() {
        // 真实窗口 128K：用掉 60K 后剩 ~68K
        manager.updateAllocation(conversationHistory = 60_000)
        assertTrue(manager.canFit(50_000))
        assertFalse(manager.canFit(200_000))
    }

    @Test
    fun `yellow warning at 80 percent`() {
        manager.updateAllocation(conversationHistory = AppConstants.CONTEXT_WARNING_YELLOW)
        assertTrue(manager.needsCompression())
        assertEquals(ContextWarningLevel.YELLOW, manager.budget.value.warningLevel)
    }

    @Test
    fun `red warning at 95 percent`() {
        manager.updateAllocation(conversationHistory = AppConstants.CONTEXT_WARNING_RED)
        assertTrue(manager.needsUrgentCompression())
        assertEquals(ContextWarningLevel.RED, manager.budget.value.warningLevel)
    }

    @Test
    fun `estimate tokens is reasonable`() {
        val tokens = manager.estimateTokens("Hello, world!")
        assertTrue(tokens in 1..10)
    }

    @Test
    fun `estimate tokens for Chinese text`() {
        val tokens = manager.estimateTokens("你好世界这是一段中文文本")
        assertTrue(tokens > 0)
    }

    @Test
    fun `reset clears all state`() {
        manager.updateAllocation(systemPrompt = 3000, conversationHistory = 100_000)
        manager.reset()

        val budget = manager.budget.value
        assertEquals(0L, budget.totalUsed)
        assertEquals(ContextWarningLevel.OK, budget.warningLevel)
    }
}
