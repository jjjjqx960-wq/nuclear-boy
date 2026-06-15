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
    fun detectApiMutationRequest() {
        val hint = detectToolActionDraftHint("你走 API 给我加进去呢")

        assertNotNull(hint)
        assertTrue(hint?.summary.orEmpty().contains("调用接口/API"))
        assertTrue(hint?.summary.orEmpty().contains("不能真实调用接口"))
        assertTrue(hint?.evidenceTargets.orEmpty().contains("接口/API 调用记录"))
        assertTrue(hint?.semantics.orEmpty().contains("工具真实执行"))
    }

    @Test
    fun detectNaturalGatewayConfigRequest() {
        val hint = detectToolActionDraftHint("后台加一下这个模型到网关里")

        assertNotNull(hint)
        assertTrue(hint?.summary.orEmpty().contains("调用接口/API"))
        assertTrue(hint?.summary.orEmpty().contains("不能真实调用接口"))
        assertTrue(hint?.evidenceTargets.orEmpty().contains("远程配置变更记录"))
    }

    @Test
    fun detectColloquialGatewayJoinRequest() {
        val hint = detectToolActionDraftHint("帮我把这个模型加入网关")

        assertNotNull(hint)
        assertTrue(hint?.summary.orEmpty().contains("调用接口/API"))
        assertTrue(hint?.summary.orEmpty().contains("不能真实调用接口"))
        assertTrue(hint?.evidenceTargets.orEmpty().contains("接口/API 调用记录"))
    }

    @Test
    fun ignoreApiConceptQuestion() {
        val hint = detectToolActionDraftHint("API 是什么")

        assertNull(hint)
    }

    @Test
    fun ignoreApiLearningQuestion() {
        val hint = detectToolActionDraftHint("怎么调用接口添加模型")

        assertNull(hint)
    }

    @Test
    fun ignoreApiParameterLearningQuestion() {
        val hint = detectToolActionDraftHint("请调用接口时怎么带参数")

        assertNull(hint)
    }

    @Test
    fun detectApiExecutionEvenWhenTextMentionsHow() {
        val hint = detectToolActionDraftHint("你看看怎么走 API 给我加进去")

        assertNotNull(hint)
        assertTrue(hint?.summary.orEmpty().contains("调用接口/API"))
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

    @Test
    fun buildEvidenceMessageForToolRequest() {
        val evidence = buildToolActionEvidenceMessage("请读取 demo.md 并运行测试")

        assertNotNull(evidence)
        assertTrue(evidence.orEmpty().contains("本轮工具能力提示"))
        assertTrue(evidence.orEmpty().contains("可见工具执行卡"))
        assertTrue(evidence.orEmpty().contains("工具受限，未真实执行"))
    }

    @Test
    fun buildEvidenceMessageForApiRequest() {
        val evidence = buildToolActionEvidenceMessage("走 API 把这个模型配置同步到后台")

        assertNotNull(evidence)
        assertTrue(evidence.orEmpty().contains("调用接口/API"))
        assertTrue(evidence.orEmpty().contains("接口/API 调用记录"))
        assertTrue(evidence.orEmpty().contains("远程配置变更记录"))
        assertTrue(evidence.orEmpty().contains("没有这些证据就不要当作已完成"))
    }

    @Test
    fun keepApiEvidenceWhenRealityGuardAppended() {
        val guarded = appendToolRealityGuard("你走 API 给我加进去呢")
        val evidence = buildToolActionEvidenceMessage(guarded)

        assertNotNull(evidence)
        assertTrue(evidence.orEmpty().contains("调用接口/API"))
        assertTrue(evidence.orEmpty().contains("接口/API 调用记录"))
        assertTrue(evidence.orEmpty().contains("不能真实调用接口"))
        assertTrue(!evidence.orEmpty().contains("读取/查看文件"))
    }

    @Test
    fun skipEvidenceMessageForOrdinaryChat() {
        val evidence = buildToolActionEvidenceMessage("帮我写一个睡前故事")

        assertNull(evidence)
    }

    @Test
    fun buildModelGuardForToolRequest() {
        val guard = buildToolActionModelGuard("请创建文件 demo.md 并运行测试")

        assertNotNull(guard)
        assertTrue(guard.orEmpty().contains("本轮工具真实性约束"))
        assertTrue(guard.orEmpty().contains("工具受限，未真实执行"))
        assertTrue(guard.orEmpty().contains("不得声称已经读取、写入、运行"))
    }

    @Test
    fun buildModelGuardForApiRequest() {
        val guard = buildToolActionModelGuard("请调用接口把模型配置同步到后台")

        assertNotNull(guard)
        assertTrue(guard.orEmpty().contains("不得声称已经读取、写入、运行、安装、测试、验证、调用接口、提交请求或修改远程配置"))
        assertTrue(guard.orEmpty().contains("接口/API 调用记录"))
        assertTrue(guard.orEmpty().contains("远程配置变更记录"))
    }

    @Test
    fun skipModelGuardForOrdinaryChat() {
        val guard = buildToolActionModelGuard("帮我写一个睡前故事")

        assertNull(guard)
    }

    @Test
    fun buildMissingEvidenceReviewWhenToolRequestHasNoEvidence() {
        val review = buildToolActionMissingEvidenceReview(
            userText = "请读取 demo.md 并运行测试",
            assistantText = "我已经检查完了，结果正常。",
            hasVisibleToolEvidence = false,
        )

        assertNotNull(review)
        assertTrue(review.orEmpty().contains("本轮结果复核"))
        assertTrue(review.orEmpty().contains("未看到工具执行卡"))
        assertTrue(review.orEmpty().contains("不要把本轮回复当作已完成结果"))
    }

    @Test
    fun buildMissingEvidenceReviewWhenApiRequestHasNoEvidence() {
        val review = buildToolActionMissingEvidenceReview(
            userText = "走 API 把这个模型配置同步到后台",
            assistantText = "已经加好了，可以直接用了。",
            hasVisibleToolEvidence = false,
        )

        assertNotNull(review)
        assertTrue(review.orEmpty().contains("调用接口/API"))
        assertTrue(review.orEmpty().contains("接口/API 调用记录"))
        assertTrue(review.orEmpty().contains("远程配置变更记录"))
        assertTrue(review.orEmpty().contains("未看到工具执行卡"))
    }

    @Test
    fun skipMissingEvidenceReviewWhenToolEvidenceExists() {
        val review = buildToolActionMissingEvidenceReview(
            userText = "请读取 demo.md 并运行测试",
            assistantText = "已根据工具结果完成。",
            hasVisibleToolEvidence = true,
        )

        assertNull(review)
    }

    @Test
    fun skipMissingEvidenceReviewWhenAssistantDeclaresLimitation() {
        val review = buildToolActionMissingEvidenceReview(
            userText = "请读取 demo.md 并运行测试",
            assistantText = "工具受限，未真实执行。需要切换支持 tools 的模型。",
            hasVisibleToolEvidence = false,
        )

        assertNull(review)
    }

    @Test
    fun skipMissingEvidenceReviewForOrdinaryChat() {
        val review = buildToolActionMissingEvidenceReview(
            userText = "帮我写一个睡前故事",
            assistantText = "从前有一颗星星。",
            hasVisibleToolEvidence = false,
        )

        assertNull(review)
    }

    private fun String.countOccurrences(needle: String): Int =
        split(needle).size - 1
}
