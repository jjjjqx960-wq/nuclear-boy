package com.nuclearboy.ui.chat.parts

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolActionDraftHintTest {

    @Test
    fun detectFileWriteRequest() {
        val hint = detectToolActionDraftHint("请创建文件 skills/demo/SKILL.md 并写入说明")

        assertNotNull(hint)
        assertTrue(hint?.summary.orEmpty().contains("写入/创建文件"))
        assertTrue(hint?.semantics.orEmpty().contains("工具真实执行"))
    }

    @Test
    fun detectCommandRunRequest() {
        val hint = detectToolActionDraftHint("帮我运行 ./gradlew assembleDebug 并验证结果")

        assertNotNull(hint)
        assertTrue(hint?.summary.orEmpty().contains("运行命令或脚本"))
    }

    @Test
    fun ignoreOrdinaryChatRequest() {
        val hint = detectToolActionDraftHint("帮我写一个睡前故事")

        assertNull(hint)
    }

    @Test
    fun appendRealityGuardOnce() {
        val guarded = appendToolRealityGuard("请读取 demo.md")

        assertTrue(guarded.contains("工具受限，未真实执行"))
        assertTrue(guarded.contains("不要编造已读取"))
        assertTrue(appendToolRealityGuard(guarded).countOccurrences("工具受限，未真实执行") == 1)
    }

    private fun String.countOccurrences(needle: String): Int =
        split(needle).size - 1
}
