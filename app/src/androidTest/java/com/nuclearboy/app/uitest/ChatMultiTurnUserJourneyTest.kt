package com.nuclearboy.app.uitest

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMultiTurnUserJourneyTest {
    private val robot = ChatJourneyRobot()

    @Test
    fun userCanCompleteMultiTurnFrontendConversation() {
        robot.prepareFreshConversation()
        val session = "soak${System.currentTimeMillis()}"
        val evidence = multiTurnPrompts(session).mapIndexed { index, prompt ->
            robot.sendPromptAndWait(
                prompt = prompt,
                label = "多轮对话第${index + 1}轮",
                timeoutMs = 180_000,
                requireVisibleAssistantIncrease = index == 0,
            )
        }

        robot.assertNoKnownChatFailure()
        robot.assertNoSelfReportedConversationProblem()
        robot.assertAppInForeground("多轮对话完成后")
        assertTrue(
            "多轮真实对话应至少观察到一次工具活动，避免只测纯文本 ping：$evidence",
            evidence.any { it.toolMarkerVisible },
        )
    }

    private fun multiTurnPrompts(session: String): List<String> = listOf(
        "回合1-$session：请用一句话确认你能正常回复，并说明你会按用户上下文继续对话。",
        "回合2-$session：请使用可用文件工具查看当前项目目录，只总结你看到的关键文件。",
        "回合3-$session：继续使用工具检查 README 或 calculator.py 是否存在，只回答结果和下一步建议。",
        "回合4-$session：基于前面两次工具结果，给出一个少于40字的用户视角改进建议。",
        "回合5-$session：请回忆刚才我连续问了什么，按1到4列出要点，不能重新开始话题。",
        "回合6-$session：最终确认当前对话是否出现空回复、重复错误或卡住；只给结论和一个理由。",
    )
}
