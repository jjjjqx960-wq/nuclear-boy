package com.nuclearboy.agent

import com.nuclearboy.common.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemPromptBuilderTest {

    @Test
    fun `custom instructions are injected when provided`() {
        val prompt = SystemPromptBuilder.build(
            userProfile = UserProfile(),
            customInstructions = "请始终用英文回复并且简短",
        )
        assertTrue(prompt.contains("用户自定义指令"))
        assertTrue(prompt.contains("请始终用英文回复并且简短"))
    }

    @Test
    fun `no custom instructions section when blank`() {
        val prompt = SystemPromptBuilder.build(userProfile = UserProfile(), customInstructions = "   ")
        assertFalse(prompt.contains("用户自定义指令"))
    }

    @Test
    fun `identity always present`() {
        val prompt = SystemPromptBuilder.build(userProfile = UserProfile())
        assertTrue(prompt.contains("核弹男孩"))
    }
}
