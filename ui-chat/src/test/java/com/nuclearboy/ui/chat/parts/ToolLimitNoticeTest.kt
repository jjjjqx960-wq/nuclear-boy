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
    fun detectEnglishToolLimitMessageCaseInsensitive() {
        val notice = detectToolLimitNotice(
            "Tool calls are NOT supported by this gateway, so I cannot READ_FILE or RUN_PYTHON. No real execution happened.",
        )

        assertNotNull(notice)
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("tools/function_call") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
    }

    @Test
    fun detectModelSelfDisclaimerAboutFileAndCommandAccess() {
        val notice = detectToolLimitNotice(
            "I don't have access to files or the local filesystem here, so I can't run commands or edit files in your project.",
        )

        assertNotNull(notice)
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("不要把本轮当作已执行") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
    }

    @Test
    fun detectModelSelfDisclaimerAboutNetworkGithubAdbAndSshAccess() {
        val notice = detectToolLimitNotice(
            "I cannot browse the internet, access GitHub, connect to your server, or use ADB/SSH on your Android device from here.",
        )

        assertNotNull(notice)
        assertTrue(notice?.summary.orEmpty().contains("ADB/SSH"))
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.semantics.orEmpty().contains("外部操作能力"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("不要把本轮当作已执行") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
    }

    @Test
    fun detectModelSelfDisclaimerAboutPhoneUiControl() {
        val notice = detectToolLimitNotice(
            "I can't interact with your screen, click buttons, open apps, install apps, or control your phone directly from here.",
        )

        assertNotNull(notice)
        assertTrue(notice?.summary.orEmpty().contains("操作屏幕/App"))
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.semantics.orEmpty().contains("前端控制"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("不要把本轮当作已执行") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
    }

    @Test
    fun detectModelOnlyGuidanceDisclaimerForExecutionTask() {
        val notice = detectToolLimitNotice(
            "I can only provide guidance and steps here; I can't perform actions for you, run tests, or make changes directly in the app.",
        )

        assertNotNull(notice)
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.semantics.orEmpty().contains("未真实执行"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("不要把本轮当作已执行") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
    }

    @Test
    fun detectModelCannotVerifyOrRunTestsDisclaimer() {
        val notice = detectToolLimitNotice(
            "I can't verify the fix or run the tests because I don't have a runtime environment for your app here.",
        )

        assertNotNull(notice)
        assertTrue(notice?.summary.orEmpty().contains("没有真实发生"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("不要把本轮当作已执行") })
        assertEquals("tool.protocol", notice?.diagnosticLabel)
    }

    @Test
    fun ignoreOrdinaryModelError() {
        val notice = detectToolLimitNotice("处理时遇到了问题：HTTP 404: model not found")

        assertNull(notice)
    }
}
