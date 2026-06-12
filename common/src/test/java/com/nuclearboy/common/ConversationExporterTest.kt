package com.nuclearboy.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationExporterTest {

    private fun msg(role: MessageRole, content: String, tools: List<ToolCallRecord> = emptyList()) =
        ChatMessage(role = role, content = content, toolCalls = tools)

    @Test
    fun `exports user and assistant turns with headings`() {
        val md = ConversationExporter.toMarkdown(
            "我的项目",
            listOf(msg(MessageRole.USER, "你好"), msg(MessageRole.ASSISTANT, "搞定了 ✨")),
        )
        assertTrue(md.startsWith("# 我的项目"))
        assertTrue(md.contains("## 你\n\n你好"))
        assertTrue(md.contains("## 核弹男孩\n\n搞定了 ✨"))
    }

    @Test
    fun `skips system and empty messages`() {
        val md = ConversationExporter.toMarkdown(
            "t",
            listOf(
                msg(MessageRole.SYSTEM, "系统提示"),
                msg(MessageRole.USER, "   "),
                msg(MessageRole.ASSISTANT, "回答"),
            ),
        )
        assertFalse(md.contains("系统提示"))
        assertFalse(md.contains("## 系统"))
        assertTrue(md.contains("## 核弹男孩"))
        assertFalse(md.contains("## 你")) // 空用户消息被跳过
    }

    @Test
    fun `includes tool calls compactly with truncation`() {
        val longOut = "x".repeat(500)
        val md = ConversationExporter.toMarkdown(
            "t",
            listOf(msg(MessageRole.ASSISTANT, "执行中",
                listOf(ToolCallRecord(toolName = "pc_cli_run", input = "{}", output = longOut)))),
        )
        assertTrue(md.contains("**工具调用：**"))
        assertTrue(md.contains("`pc_cli_run`"))
        assertTrue(md.contains("…")) // 长输出被截断
    }

    @Test
    fun `tool-only message without content still exported`() {
        val md = ConversationExporter.toMarkdown(
            "t",
            listOf(msg(MessageRole.ASSISTANT, "",
                listOf(ToolCallRecord(toolName = "pc_read_file", input = "{}", output = "data")))),
        )
        assertTrue(md.contains("`pc_read_file`"))
    }

    @Test
    fun `date label included when provided`() {
        val md = ConversationExporter.toMarkdown("t", listOf(msg(MessageRole.USER, "hi")), dateLabel = "2026-06-12")
        assertTrue(md.contains("导出时间：2026-06-12"))
    }

    @Test
    fun `empty conversation gives placeholder`() {
        val md = ConversationExporter.toMarkdown("t", emptyList())
        assertTrue(md.contains("没有可导出的对话内容"))
    }

    @Test
    fun `default title when blank`() {
        val md = ConversationExporter.toMarkdown("", listOf(msg(MessageRole.USER, "hi")))
        assertTrue(md.startsWith("# 核弹男孩对话"))
    }
}
