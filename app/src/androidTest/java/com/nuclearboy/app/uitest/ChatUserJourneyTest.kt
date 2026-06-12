package com.nuclearboy.app.uitest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatUserJourneyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val appPackageName = "com.nuclearboy.app.debug"

    @Test
    fun userCanSendFormalChatFromFrontend() {
        resetConversationHistory()
        configureProviderFromInstrumentationArgs()
        launchApp()
        dismissCommonSystemPrompts()

        waitForObject("聊天输入框", 30_000) {
            device.findObject(By.clazz("android.widget.EditText"))
                ?: device.findObject(By.desc("聊天输入框"))
                ?: device.findObject(By.textContains("输入指令"))
                ?: device.findObject(By.textContains("和核弹男孩对话"))
        }

        val prompt = "uie2e${System.currentTimeMillis()}"
        val assistantCountBefore = device.findObjects(By.descContains("核弹男孩消息")).size
        focusChatInputWithKeyboard()
        clearInputText()
        shellInputText(prompt)

        assertTrue("输入后输入框应持有测试消息", waitUntil(10_000) {
            focusedInputText()?.contains(prompt) == true
        })

        val send = waitForObject("发送消息按钮", 10_000) {
            device.findObject(By.desc("发送消息"))
        }
        tapObject(send)

        assertTrue("点击发送后输入框应被清空，不能停留在草稿态", waitUntil(10_000) {
            focusedInputText()?.contains(prompt) != true
        })

        waitForChatCompletion(assistantCountBefore, 120_000)

        assertNoKnownChatFailure()
        val assistantCountAfter = device.findObjects(By.descContains("核弹男孩消息")).size
        assertTrue(
            "正式聊天结束后应新增助手消息，before=$assistantCountBefore after=$assistantCountAfter",
            assistantCountAfter > assistantCountBefore,
        )
    }

    /**
     * 允许通过 instrumentation 参数预置 OpenAI 兼容网关，让全新安装也能跑通正式聊天：
     * -Pandroid.testInstrumentationRunnerArguments.nbBaseUrl=https://网关/v1
     * -Pandroid.testInstrumentationRunnerArguments.nbModel=模型名
     * -Pandroid.testInstrumentationRunnerArguments.nbApiKey=可选Key
     * 测试与 App 同进程，ApiKeyManager 读写同一份加密 prefs，配置立即生效。
     */
    private fun configureProviderFromInstrumentationArgs() {
        val args = InstrumentationRegistry.getArguments()
        val baseUrl = args.getString("nbBaseUrl")?.trim().orEmpty()
        val model = args.getString("nbModel")?.trim().orEmpty()
        if (baseUrl.isBlank() || model.isBlank()) return
        val apiKey = args.getString("nbApiKey")?.trim().orEmpty()
        val manager = com.nuclearboy.api.deepseek.ApiKeyManager(instrumentation.targetContext)
        manager.setCustomProviderConfig(baseUrl = baseUrl, modelName = model, apiKey = apiKey)
    }

    /**
     * 清掉残留会话，保证每次都从空对话开始单轮测试——
     * 否则历史越积越多，prompt 变大、正式聊天变慢，超时阈值会误判失败。
     */
    private fun resetConversationHistory() {
        device.executeShellCommand(
            "su -c rm -f /storage/emulated/0/Android/data/$appPackageName/files/NuclearBoy/__general__/.agent/conversation.json",
        )
    }

    private fun launchApp() {
        wakeAndUnlockScreen()
        val activityName = "$appPackageName/com.nuclearboy.app.MainActivity"
        val launchResult = device.executeShellCommand("am start -W -n $activityName")
        assertFalse(
            "目标 App 启动命令不应失败：$launchResult",
            launchResult.contains("Error", ignoreCase = true) ||
                launchResult.contains("Exception", ignoreCase = true),
        )
        device.waitForIdle(5_000)
        assertTrue("启动后目标 App 应处于前台：${focusedWindowSummary()}", waitUntil(10_000) {
            isAppInForeground()
        })
    }

    private fun wakeAndUnlockScreen() {
        // 设备可能息屏锁屏：UIAutomator 看不到任何 UI，必须先亮屏解锁。
        // 本 ROM 禁止 shell 注入，亮屏走 root；再解锁 keyguard 并保持测试期间常亮。
        runCatching { device.wakeUp() }
        executeRootInput("input keyevent KEYCODE_WAKEUP")
        device.executeShellCommand("su -c wm dismiss-keyguard")
        device.executeShellCommand("su -c svc power stayon true")
        device.waitForIdle(2_000)
    }

    private fun dismissCommonSystemPrompts() {
        listOf("允许", "确定", "OK", "Allow").forEach { label ->
            device.findObject(By.text(label))?.let(::tapObject)
        }
    }

    private fun focusChatInputWithKeyboard() {
        assertAppInForeground("聚焦聊天输入前")
        repeat(24) {
            if (focusedInputText() != null) return
            executeRootInput("input keyevent KEYCODE_DPAD_DOWN")
            Thread.sleep(120)
        }
        if (focusedInputText() != null) return

        val input = waitForObject("聊天输入框", 5_000) {
            device.findObject(By.clazz("android.widget.EditText"))
                ?: device.findObject(By.desc("聊天输入框"))
        }
        tapObject(input)
        assertTrue("聊天输入框应能通过键盘导航或点击获得焦点", waitUntil(5_000) {
            focusedInputText() != null
        })
    }

    private fun tapObject(target: UiObject2) {
        assertAppInForeground("执行点击前")
        val bounds = target.visibleBounds
        executeRootInput("input tap ${bounds.centerX()} ${bounds.centerY()}")
        device.waitForIdle(1_000)
    }

    private fun shellInputText(text: String) {
        assertAppInForeground("输入文本前")
        // executeShellCommand 走 Runtime.exec，按空格分词且不解析引号，
        // 引号会被原样输入到文本框，所以只做 %s 空格编码、不能加引号包裹。
        val safeText = text.replace(" ", "%s")
        executeRootInput("input text $safeText")
        device.waitForIdle(1_000)
    }

    private fun clearInputText() {
        assertAppInForeground("清空输入框前")
        executeRootInput("input keyevent KEYCODE_MOVE_END")
        repeat(64) {
            executeRootInput("input keyevent KEYCODE_DEL")
        }
        device.waitForIdle(1_000)
    }

    private fun executeRootInput(command: String) {
        // 同样因为 Runtime.exec 不解析引号：su -c 后跟多段参数时 su 会自行拼接，
        // 若额外包一层引号，sh 会把带引号的整串当作单个命令名执行而失败。
        val result = device.executeShellCommand("su -c $command")
        assertFalse(
            "root input 命令不应被系统拦截或失败：$result",
            result.contains("SecurityException", ignoreCase = true) ||
                result.contains("INJECT_EVENTS", ignoreCase = true) ||
                result.contains("not found", ignoreCase = true) ||
                result.contains("Permission denied", ignoreCase = true) ||
                result.contains("inaccessible", ignoreCase = true),
        )
    }

    private fun waitForChatCompletion(assistantCountBefore: Int, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        do {
            assertNoKnownChatFailure()
            val assistantCountAfter = device.findObjects(By.descContains("核弹男孩消息")).size
            val processing = device.hasObject(By.desc("停止"))
            if (assistantCountAfter > assistantCountBefore && !processing) {
                return
            }
            Thread.sleep(500)
        } while (System.currentTimeMillis() < deadline)
        fail("正式聊天应在 ${timeoutMs / 1000} 秒内完成并产生助手回复")
    }

    private fun assertNoKnownChatFailure() {
        val knownFailures = listOf(
            "没能生成回复",
            "请求格式有误",
            "内部数据可能不一致",
            "模型路由失败",
            "正式聊天请求失败",
            "处理时遇到了问题",
            "需要配置 DeepSeek API Key",
            "没有可用模型",
            "空回复",
        )
        knownFailures.forEach { failureText ->
            assertFalse(
                "界面不应出现已知聊天失败文案：$failureText",
                device.hasObject(By.textContains(failureText)),
            )
        }
    }

    private fun focusedInputText(): String? =
        device.findObject(By.focused(true).clazz("android.widget.EditText"))?.text

    private fun assertAppInForeground(stage: String) {
        assertTrue("$stage，当前前台必须是目标 App：${focusedWindowSummary()}", isAppInForeground())
    }

    private fun isAppInForeground(): Boolean {
        val focus = focusedWindowSummary()
        return focus.contains(appPackageName)
    }

    private fun focusedWindowSummary(): String {
        val dump = device.executeShellCommand("dumpsys window")
        val focusLines = dump.lineSequence()
            .filter {
                it.contains("mCurrentFocus") ||
                    it.contains("mFocusedApp") ||
                    it.contains("mInputMethodTarget")
            }
            .take(4)
            .joinToString(" | ") { it.trim() }
        return focusLines.take(500)
    }

    private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        do {
            if (predicate()) return true
            Thread.sleep(250)
        } while (System.currentTimeMillis() < deadline)
        return false
    }

    private fun waitForObject(
        label: String,
        timeoutMs: Long,
        finder: () -> UiObject2?,
    ): UiObject2 {
        val deadline = System.currentTimeMillis() + timeoutMs
        do {
            finder()?.let { return it }
            Thread.sleep(250)
        } while (System.currentTimeMillis() < deadline)
        fail("等待 $label 超时")
        throw AssertionError("unreachable")
    }
}
