package com.nuclearboy.app.uitest

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkillPromptGenerationJourneyTest {
    private val robot = ChatJourneyRobot()

    @Test
    fun appConversationCanGenerateSkillAndOptimizePrompt() {
        robot.prepareFreshConversation()
        robot.device.executeShellCommand(
            "su -c rm -rf /storage/emulated/0/Android/data/${robot.appPackageName}/files/NuclearBoy/skills/app-dialog-smoke",
        )

        val session = "skill${System.currentTimeMillis()}"
        val evidence = listOf(
            "回合1-$session：这是一条自动化实测，已授权你在当前项目内创建或覆盖测试文件。请使用写文件工具创建 skills/app-dialog-smoke/SKILL.md。内容必须包含 marker：APP_DIALOG_SMOKE_SKILL=OK、name: app-dialog-smoke、description、steps。完成后只回答写入路径。",
            "回合2-$session：请先真实调用 read_file 读取 skills/app-dialog-smoke/SKILL.md，然后真实调用 write_file 写入 skills/app-dialog-smoke/system-prompt.md。内容必须包含 marker：APP_DIALOG_SMOKE_PROMPT=OK，并强调真实前端实测、不要只做 ping、保留工具结果上下文。若当前没有真实工具调用能力，禁止输出文件内容或声称完成，只回答：工具受限，未写入。",
            "回合3-$session：请再次读取这两个文件，确认 skill 和系统提示词能配合工作。如果没有问题，只回答“验证通过”和一个理由；不要重新开始话题。",
        ).mapIndexed { index, prompt ->
            robot.sendPromptAndWait(
                prompt = prompt,
                label = "skill/prompt 对话第${index + 1}轮",
                timeoutMs = 240_000,
                requireVisibleAssistantIncrease = index == 0,
            )
        }

        robot.assertNoKnownChatFailure()
        robot.assertNoSelfReportedConversationProblem()
        assertTrue(
            "skill/prompt 生成旅程应至少观察到一次工具活动：$evidence",
            evidence.any { it.toolMarkerVisible },
        )

        val skill = readWorkspaceFile("skills/app-dialog-smoke/SKILL.md")
        val prompt = readWorkspaceFile("skills/app-dialog-smoke/system-prompt.md")
        if (!skill.contains("APP_DIALOG_SMOKE_SKILL=OK")) {
            val assistantText = readAssistantConversationText()
            assertTrue(
                "工具不可用时不能伪造 SKILL.md 写入，应明确说明工具受限：$assistantText",
                assistantText.isToolUnavailableNotice(),
            )
            return
        }
        assertTrue("SKILL.md 应真实写入 marker", skill.contains("APP_DIALOG_SMOKE_SKILL=OK"))
        assertTrue("SKILL.md 应声明 app-dialog-smoke", skill.contains("app-dialog-smoke"))

        if (prompt.contains("APP_DIALOG_SMOKE_PROMPT=OK")) {
            assertTrue("system-prompt.md 应包含真实前端实测要求", prompt.contains("真实前端实测"))
        } else {
            val assistantText = readAssistantConversationText()
            assertTrue(
                "工具不可用时不能伪造 system-prompt 写入，应明确说明工具受限：$assistantText",
                assistantText.isToolUnavailableNotice(),
            )
        }
    }

    private fun readWorkspaceFile(relativePath: String): String {
        val path = "/storage/emulated/0/Android/data/${robot.appPackageName}/files/NuclearBoy/$relativePath"
        return robot.device.executeShellCommand("su -c cat $path")
    }

    private fun readAssistantConversationText(): String {
        val raw = robot.device.executeShellCommand(
            "su -c cat /storage/emulated/0/Android/data/${robot.appPackageName}/files/NuclearBoy/__general__/.agent/conversation.json",
        )
        val messages = JSONArray(raw)
        return buildString {
            for (index in 0 until messages.length()) {
                val message = messages.getJSONObject(index)
                if (message.optString("role").equals("ASSISTANT", ignoreCase = true)) {
                    appendLine(message.optString("content"))
                    appendLine(message.optString("reasoningContent"))
                }
            }
        }
    }

    private fun String.isToolUnavailableNotice(): Boolean =
        contains("工具受限") ||
            contains("当前网关不支持工具调用") ||
            contains("未真实执行") ||
            contains("尚未真实执行") ||
            contains("未写入")
}
