package com.nuclearboy.ui.chat.parts

import org.junit.Assert.assertEquals
import org.junit.Test

class FileReferencePromptTest {
    @Test
    fun `multiple file reference prompt uses project relative paths`() {
        val prompt = buildFileReferencesPrompt(
            filePaths = listOf(
                "D:\\project\\app\\src\\Main.kt",
                "D:\\project\\README.md",
            ),
            projectRoot = "D:\\project",
        )

        assertEquals(
            "请阅读以下项目文件，并结合我的要求继续处理：\n" +
                "- `app/src/Main.kt`\n" +
                "- `README.md`",
            prompt,
        )
    }

    @Test
    fun `multiple file reference prompt removes duplicates`() {
        val prompt = buildFileReferencesPrompt(
            filePaths = listOf(
                "D:\\project\\README.md",
                "D:\\project\\README.md",
            ),
            projectRoot = "D:\\project",
        )

        assertEquals("请阅读项目文件 `README.md`，并结合我的要求继续处理：", prompt)
    }

    @Test
    fun `toggle selected file path adds and removes path`() {
        val selected = toggleSelectedFilePath(emptyList(), "app/src/Main.kt")
        val cleared = toggleSelectedFilePath(selected, "app/src/Main.kt")

        assertEquals(listOf("app/src/Main.kt"), selected)
        assertEquals(emptyList<String>(), cleared)
    }
}
