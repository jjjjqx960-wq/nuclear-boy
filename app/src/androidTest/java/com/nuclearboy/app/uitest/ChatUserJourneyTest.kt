package com.nuclearboy.app.uitest

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatUserJourneyTest {
    private val robot = ChatJourneyRobot()

    @Test
    fun userCanSendFormalChatFromFrontend() {
        robot.prepareFreshConversation()
        val prompt = "uie2e${System.currentTimeMillis()}"
        robot.sendPromptAndWait(
            prompt = prompt,
            label = "单轮正式聊天",
            timeoutMs = 120_000,
            requireVisibleAssistantIncrease = true,
        )
    }
}
