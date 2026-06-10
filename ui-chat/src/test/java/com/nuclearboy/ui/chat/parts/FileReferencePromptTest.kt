package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo
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

    @Test
    fun `select visible file paths skips directories and keeps existing selection`() {
        val selected = selectVisibleFilePaths(
            selectedPaths = listOf("README.md"),
            visibleFiles = listOf(
                FileInfo(path = "app/src", name = "src", isDirectory = true),
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt"),
                FileInfo(path = "README.md", name = "README.md", extension = "md"),
            ),
        )

        assertEquals(listOf("README.md", "app/src/Main.kt"), selected)
    }

    @Test
    fun `unselect visible file paths skips directories and keeps hidden selection`() {
        val selected = unselectVisibleFilePaths(
            selectedPaths = listOf("README.md", "app/src/Main.kt", "app/src/Hidden.kt"),
            visibleFiles = listOf(
                FileInfo(path = "app/src", name = "src", isDirectory = true),
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt"),
                FileInfo(path = "README.md", name = "README.md", extension = "md"),
            ),
        )

        assertEquals(listOf("app/src/Hidden.kt"), selected)
    }

    @Test
    fun `selected file panel entries returns selected files only`() {
        val entries = selectedFilePanelEntries(
            files = listOf(
                FileInfo(path = "app/src", name = "src", isDirectory = true),
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt"),
                FileInfo(path = "README.md", name = "README.md", extension = "md"),
            ),
            selectedPaths = listOf("app/src", "README.md"),
        )

        assertEquals(listOf("README.md"), entries.map { it.path })
    }
}
