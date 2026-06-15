package com.nuclearboy.ui.chat.parts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolMissingEvidenceReviewNoticeTest {

    @Test
    fun detectsMissingEvidenceReview() {
        val notice = detectToolMissingEvidenceReviewNotice(
            "本轮结果复核：需要读取文件和运行测试\n未看到工具执行卡、文件变更卡，也未看到明确的“工具受限，未真实执行”说明；请先不要把本轮回复当作已完成结果。",
        )

        assertNotNull(notice)
        assertEquals("本轮结果复核", notice?.title)
        assertTrue(notice?.summary.orEmpty().contains("不要把本轮回复当作已完成结果"))
        assertTrue(notice?.actions.orEmpty().any { it.contains("tools/function_call") })
        assertTrue(notice?.semantics.orEmpty().contains("结果复核提示"))
    }

    @Test
    fun ignoresOrdinarySystemMessage() {
        val notice = detectToolMissingEvidenceReviewNotice("本轮工具能力提示：请以工具卡为准")

        assertNull(notice)
    }
}
