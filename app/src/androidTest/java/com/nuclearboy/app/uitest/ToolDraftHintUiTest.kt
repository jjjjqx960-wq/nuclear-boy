package com.nuclearboy.app.uitest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.nuclearboy.api.deepseek.ApiKeyManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolDraftHintUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val robot = ChatJourneyRobot()

    @Test
    fun customProviderToolDraftShowsPreSendHint() {
        ApiKeyManager(instrumentation.targetContext).setCustomProviderConfig(
            baseUrl = "http://127.0.0.1:1/v1",
            modelName = "local/tool-draft-hint",
            apiKey = "",
        )
        robot.resetConversationHistory()
        robot.launchApp()

        robot.waitForChatInput(30_000)
        robot.enterDraftText("请读取 skills/app-dialog-smoke/SKILL.md 并运行验证")

        assertTrue("工具型草稿应在发送前显示工具能力预警语义", waitUntil(10_000) {
            device.hasObject(By.descContains("工具能力预警"))
        })
        assertTrue("工具型草稿预警标题应可见", device.hasObject(By.textContains("可能需要工具能力")))

        val appendGuard = device.findObject(By.desc("追加防假执行提示"))
            ?: device.findObject(By.text("追加防假执行提示"))
        assertTrue("工具型草稿预警应提供追加防假执行提示操作", appendGuard != null)
        appendGuard?.click()

        val input = robot.waitForChatInput(10_000)
        assertTrue("追加后输入框应包含防假执行约束", waitUntil(10_000) {
            input.text.orEmpty().contains("工具受限，未真实执行") &&
                input.text.orEmpty().contains("不要编造已读取")
        })
    }

    private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        do {
            if (predicate()) return true
            Thread.sleep(250)
        } while (System.currentTimeMillis() < deadline)
        return false
    }
}
