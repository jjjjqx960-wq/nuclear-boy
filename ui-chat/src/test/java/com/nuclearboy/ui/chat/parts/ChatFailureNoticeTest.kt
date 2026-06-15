package com.nuclearboy.ui.chat.parts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFailureNoticeTest {

    @Test
    fun detectsInactiveProviderRouteFailure() {
        val notice = detectChatFailureNotice(
            """处理时遇到了问题：HTTP 404: {"error":{"message":"No active credentials for provider: nvidia","code":"model_not_found"}}""",
        )

        assertNotNull(notice)
        assertEquals("模型路由失败", notice?.title)
        assertTrue(notice?.summary.orEmpty().contains("nvidia"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("获取模型列表") })
        assertTrue(notice?.semantics.orEmpty().contains("上游凭证"))
    }

    @Test
    fun detectsFormatFailure() {
        val notice = detectChatFailureNotice("处理时遇到了问题：请求格式有误，内部数据可能不一致，请重启对话")

        assertNotNull(notice)
        assertEquals("聊天链路失败", notice?.title)
        assertTrue(notice?.summary.orEmpty().contains("没有生成有效回复"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("stream=true") })
    }

    @Test
    fun detectsAuthFailure() {
        val notice = detectChatFailureNotice("处理时遇到了问题：HTTP 401: unauthorized invalid api key")

        assertNotNull(notice)
        assertEquals("鉴权失败", notice?.title)
        assertTrue(notice?.actions.orEmpty().any { it.contains("API Key") })
        assertTrue(notice?.semantics.orEmpty().contains("API Key"))
    }

    @Test
    fun detectsQuotaFailure() {
        val notice = detectChatFailureNotice("处理时遇到了问题：HTTP 429: rate limit exceeded, insufficient_quota")

        assertNotNull(notice)
        assertEquals("额度或限流不足", notice?.title)
        assertTrue(notice?.actions.orEmpty().any { it.contains("余额") || it.contains("额度") })
    }

    @Test
    fun detectsNetworkFailure() {
        val notice = detectChatFailureNotice("出了一点小问题…failed to connect: timeout")

        assertNotNull(notice)
        assertEquals("网络连接失败", notice?.title)
        assertTrue(notice?.actions.orEmpty().any { it.contains("VPN") })
    }

    @Test
    fun ignoresOrdinaryToolLimitMessage() {
        val notice = detectChatFailureNotice(
            "工具受限，未真实执行。当前第三方网关不支持工具调用协议。",
        )

        assertNull(notice)
    }
}
