package com.nuclearboy.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenUsageFormatTest {

    @Test
    fun `shows input and output arrows`() {
        val u = TokenUsage(promptTokens = 1200, completionTokens = 456, totalTokens = 1656)
        assertEquals("↑1.2k ↓456", TokenUsageFormat.inline(u))
    }

    @Test
    fun `appends cache hits when present`() {
        val u = TokenUsage(promptTokens = 2000, completionTokens = 100, cachedPromptTokens = 800)
        assertEquals("↑2.0k ↓100 ·缓存800", TokenUsageFormat.inline(u))
    }

    @Test
    fun `falls back to total when no breakdown`() {
        val u = TokenUsage(totalTokens = 1500)
        assertEquals("1.5k tokens", TokenUsageFormat.inline(u))
    }

    @Test
    fun `only input`() {
        assertEquals("↑300", TokenUsageFormat.inline(TokenUsage(promptTokens = 300)))
    }

    @Test
    fun `only output`() {
        assertEquals("↓42", TokenUsageFormat.inline(TokenUsage(completionTokens = 42)))
    }
}
