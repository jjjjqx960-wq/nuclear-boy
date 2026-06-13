package com.nuclearboy.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [extractSearchTerms] 纯函数单测：中文 2-gram、ASCII 整词、混排、标点、去重、上限。
 */
class MemorySearchTermsTest {

    @Test
    fun `chinese run splits into bigrams`() {
        // "上下文管理" -> 上下 下文 文管 管理
        val terms = extractSearchTerms("上下文管理")
        assertEquals(listOf("上下", "下文", "文管", "管理"), terms)
    }

    @Test
    fun `ascii word kept whole and lowercased`() {
        val terms = extractSearchTerms("Kotlin")
        assertEquals(listOf("kotlin"), terms)
    }

    @Test
    fun `mixed chinese and ascii are separated`() {
        // 数字/字母连续段整词，中文段 bigram；二者在边界处断开
        val terms = extractSearchTerms("配置API网关")
        assertTrue("应含 ascii 词 api", terms.contains("api"))
        assertTrue("应含中文 bigram 配置", terms.contains("配置"))
        assertTrue("应含中文 bigram 网关", terms.contains("网关"))
        // "配置" 与 "网关" 不应跨过 API 连成 "置网"
        assertFalse(terms.contains("置网"))
    }

    @Test
    fun `punctuation and whitespace split runs`() {
        val terms = extractSearchTerms("怎么 配置，缓存？")
        assertTrue(terms.contains("怎么"))
        assertTrue(terms.contains("配置"))
        assertTrue(terms.contains("缓存"))
    }

    @Test
    fun `single char and too-short ascii are skipped`() {
        assertTrue(extractSearchTerms("好").isEmpty())   // 单个中文字跳过
        assertTrue(extractSearchTerms("a").isEmpty())    // 单个 ascii 跳过
        assertTrue(extractSearchTerms("！！！").isEmpty()) // 纯标点
    }

    @Test
    fun `terms are deduplicated`() {
        // "管理管理" -> 管理 理管 管理(dup) 理管(dup) 管理(dup) => 去重后 [管理, 理管]
        val terms = extractSearchTerms("管理管理")
        assertEquals(terms.toSet().size, terms.size) // 无重复
        assertTrue(terms.contains("管理"))
    }

    @Test
    fun `respects max terms cap`() {
        val long = "这是一段非常长的中文文本用来测试上限是否被正确截断处理"
        val terms = extractSearchTerms(long, maxTerms = 5)
        assertEquals(5, terms.size)
    }
}
