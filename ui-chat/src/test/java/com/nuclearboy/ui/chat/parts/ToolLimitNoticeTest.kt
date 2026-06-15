package com.nuclearboy.ui.chat.parts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolLimitNoticeTest {

    @Test
    fun detectCanonicalToolLimitMessage() {
        val notice = detectToolLimitNotice(
            "工具受限，未真实执行。当前第三方网关不支持工具调用协议，本轮不能读取、写入、运行或测试。",
        )

        assertNotNull(notice)
        assertEquals("工具受限", notice?.title)
        assertTrue(notice?.summary.orEmpty().contains("没有可用的工具调用协议"))
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("支持 tools/function_call") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
        assertEquals("正式聊天 / stream=true / 工具定义", notice?.verificationLabel)
    }

    @Test
    fun detectCompatibilityPromptLimitMessage() {
        val notice = detectToolLimitNotice(
            "当前第三方网关本轮没有可用工具调用协议，不能调用 read_file、write_file、list_directory、run_python 等工具。",
        )

        assertNotNull(notice)
        assertTrue(notice?.semantics.orEmpty().contains("未真实执行"))
    }

    @Test
    fun ignoreOrdinaryModelError() {
        val notice = detectToolLimitNotice("处理时遇到了问题：HTTP 404: model not found")

        assertNull(notice)
    }
}
