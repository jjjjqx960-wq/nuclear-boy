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
    fun `unselect hidden file paths keeps visible selected files only`() {
        val selected = unselectHiddenFilePaths(
            selectedPaths = listOf("README.md", "app/src/Main.kt", "app/src/Hidden.kt"),
            visibleFiles = listOf(
                FileInfo(path = "app/src", name = "src", isDirectory = true),
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt"),
                FileInfo(path = "README.md", name = "README.md", extension = "md"),
            ),
        )

        assertEquals(listOf("README.md", "app/src/Main.kt"), selected)
    }

    @Test
    fun `remove referenced file paths keeps unreferenced selection`() {
        val selected = removeReferencedFilePaths(
            selectedPaths = listOf("README.md", "app/src/Main.kt", "app/src/Hidden.kt"),
            referencedFiles = listOf(
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt"),
                FileInfo(path = "README.md", name = "README.md", extension = "md"),
            ),
        )

        assertEquals(listOf("app/src/Hidden.kt"), selected)
    }

    @Test
    fun `remove referenced file paths ignores referenced directories`() {
        val selected = removeReferencedFilePaths(
            selectedPaths = listOf("app/src", "app/src/Main.kt"),
            referencedFiles = listOf(
                FileInfo(path = "app/src", name = "src", isDirectory = true),
            ),
        )

        assertEquals(listOf("app/src", "app/src/Main.kt"), selected)
    }

    @Test
    fun `filter query after matched reference clears when selection remains`() {
        val query = filterQueryAfterMatchedReference(
            remainingSelectedCount = 3,
            currentQuery = "viewmodel",
        )

        assertEquals("", query)
    }

    @Test
    fun `filter query after matched reference stays when no selection remains`() {
        val query = filterQueryAfterMatchedReference(
            remainingSelectedCount = 0,
            currentQuery = "viewmodel",
        )

        assertEquals("viewmodel", query)
    }

    @Test
    fun `filter query after matched reference keeps blank query`() {
        val query = filterQueryAfterMatchedReference(
            remainingSelectedCount = 3,
            currentQuery = "",
        )

        assertEquals("", query)
    }

    @Test
    fun `file references toast message shows remaining selection`() {
        val message = fileReferencesToastMessage(
            referencedCount = 2,
            remainingSelectedCount = 5,
        )

        assertEquals("已引用 2 个文件，剩余 5 个已选", message)
    }

    @Test
    fun `file references toast message stays compact without remaining selection`() {
        val message = fileReferencesToastMessage(
            referencedCount = 2,
            remainingSelectedCount = 0,
        )

        assertEquals("已引用 2 个文件", message)
    }

    @Test
    fun `file references toast message shows referenced size`() {
        val message = fileReferencesToastMessage(
            referencedCount = 2,
            remainingSelectedCount = 0,
            referencedSizeLabel = "3 KB",
        )

        assertEquals("已引用 2 个文件 · 3 KB", message)
    }

    @Test
    fun `file references toast message shows size before remaining selection`() {
        val message = fileReferencesToastMessage(
            referencedCount = 2,
            remainingSelectedCount = 5,
            referencedSizeLabel = "3 KB",
        )

        assertEquals("已引用 2 个文件 · 3 KB，剩余 5 个已选", message)
    }

    @Test
    fun `matched reference closes panel when no selection remains`() {
        val shouldClose = shouldClosePanelAfterMatchedReference(
            remainingSelectedCount = 0,
        )

        assertEquals(true, shouldClose)
    }

    @Test
    fun `matched reference keeps panel open while selection remains`() {
        val shouldClose = shouldClosePanelAfterMatchedReference(
            remainingSelectedCount = 4,
        )

        assertEquals(false, shouldClose)
    }

    @Test
    fun `reference matched action label shows matched count`() {
        val label = fileSelectionReferenceMatchedActionLabel(
            matchedCount = 3,
        )

        assertEquals("匹配 3", label)
    }

    @Test
    fun `reference selected action label shows all count when matched action exists`() {
        val label = fileSelectionReferenceSelectedActionLabel(
            selectedCount = 7,
            hasMatchedAction = true,
        )

        assertEquals("全部 7", label)
    }

    @Test
    fun `reference selected action label shows reference count without matched action`() {
        val label = fileSelectionReferenceSelectedActionLabel(
            selectedCount = 7,
            hasMatchedAction = false,
        )

        assertEquals("引用 7", label)
    }

    @Test
    fun `reference selected action label stays compact without selection`() {
        val label = fileSelectionReferenceSelectedActionLabel(
            selectedCount = 0,
            hasMatchedAction = false,
        )

        assertEquals("引用", label)
    }

    @Test
    fun `reference visible action label shows visible count`() {
        val label = fileSelectionReferenceVisibleActionLabel(
            visibleFileCount = 5,
        )

        assertEquals("引用 5", label)
    }

    @Test
    fun `reference visible action label stays compact without visible files`() {
        val label = fileSelectionReferenceVisibleActionLabel(
            visibleFileCount = 0,
        )

        assertEquals("引用", label)
    }

    @Test
    fun `reference visible action description shows visible count`() {
        val description = fileSelectionReferenceVisibleActionDescription(
            visibleFileCount = 5,
        )

        assertEquals("引用当前可见 5 个文件到输入", description)
    }

    @Test
    fun `select visible action label shows full selectable count without visible selection`() {
        val label = fileSelectionSelectVisibleActionLabel(
            selectedVisibleCount = 0,
            visibleFileCount = 7,
        )

        assertEquals("全选 7", label)
    }

    @Test
    fun `select visible action label shows addable count with partial visible selection`() {
        val label = fileSelectionSelectVisibleActionLabel(
            selectedVisibleCount = 3,
            visibleFileCount = 7,
        )

        assertEquals("补选 4", label)
    }

    @Test
    fun `select visible action label clamps impossible counts`() {
        val label = fileSelectionSelectVisibleActionLabel(
            selectedVisibleCount = 9,
            visibleFileCount = 7,
        )

        assertEquals("补选 0", label)
    }

    @Test
    fun `selected only toggle label shows selected count without filter`() {
        val label = fileSelectionSelectedOnlyToggleLabel(
            showSelectedOnly = false,
            selectedCount = 7,
            selectedVisibleCount = 3,
            allVisibleFileCount = 11,
            hasFilterQuery = false,
        )

        assertEquals("只看 7", label)
    }

    @Test
    fun `selected only toggle label shows matched selected count with filter`() {
        val label = fileSelectionSelectedOnlyToggleLabel(
            showSelectedOnly = false,
            selectedCount = 7,
            selectedVisibleCount = 3,
            allVisibleFileCount = 11,
            hasFilterQuery = true,
        )

        assertEquals("只看 3", label)
    }

    @Test
    fun `selected only toggle label shows all filtered selectable count when active`() {
        val label = fileSelectionSelectedOnlyToggleLabel(
            showSelectedOnly = true,
            selectedCount = 7,
            selectedVisibleCount = 3,
            allVisibleFileCount = 11,
            hasFilterQuery = true,
        )

        assertEquals("全部 11", label)
    }

    @Test
    fun `clear selection action description shows selected count`() {
        val description = fileSelectionClearActionDescription(
            selectedCount = 7,
        )

        assertEquals("清空 7 个已选文件", description)
    }

    @Test
    fun `unselect visible action description shows visible selected count`() {
        val description = fileSelectionUnselectVisibleActionDescription(
            selectedVisibleCount = 3,
            showSelectedOnly = false,
        )

        assertEquals("取消当前可见 3 个已选文件", description)
    }

    @Test
    fun `unselect visible action description uses matched wording in selected only mode`() {
        val description = fileSelectionUnselectVisibleActionDescription(
            selectedVisibleCount = 2,
            showSelectedOnly = true,
        )

        assertEquals("取消匹配 2 个已选文件", description)
    }

    @Test
    fun `unselect hidden action description shows hidden selected count`() {
        val description = fileSelectionUnselectHiddenActionDescription(
            hiddenSelectedCount = 4,
        )

        assertEquals("取消隐藏 4 个已选文件", description)
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

    @Test
    fun `selected file total size skips directories and negative sizes`() {
        val totalSize = selectedFileTotalSizeBytes(
            files = listOf(
                FileInfo(path = "app/src", name = "src", size = 4096, isDirectory = true),
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt", size = 1024),
                FileInfo(path = "README.md", name = "README.md", extension = "md", size = 2048),
                FileInfo(path = "broken.bin", name = "broken.bin", extension = "bin", size = -1),
            ),
        )

        assertEquals(3072L, totalSize)
    }

    @Test
    fun `file selection total size skips directories and negative sizes`() {
        val totalSize = fileSelectionTotalSizeBytes(
            files = listOf(
                FileInfo(path = "app/src", name = "src", size = 4096, isDirectory = true),
                FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt", size = 1024),
                FileInfo(path = "README.md", name = "README.md", extension = "md", size = 2048),
                FileInfo(path = "broken.bin", name = "broken.bin", extension = "bin", size = -1),
            ),
        )

        assertEquals(3072L, totalSize)
    }

    @Test
    fun `file selection action bar stays visible when hidden selection exists`() {
        val shouldShow = shouldShowFileSelectionActionBar(
            selectedCount = 2,
            visibleFileCount = 0,
        )

        assertEquals(true, shouldShow)
    }

    @Test
    fun `file selection action bar hides when nothing is selectable or selected`() {
        val shouldShow = shouldShowFileSelectionActionBar(
            selectedCount = 0,
            visibleFileCount = 0,
        )

        assertEquals(false, shouldShow)
    }

    @Test
    fun `unselect visible action shows in normal mode with visible selection`() {
        val shouldShow = shouldShowUnselectVisibleAction(
            selectedCount = 3,
            selectedVisibleCount = 2,
            showSelectedOnly = false,
            hasFilterQuery = false,
        )

        assertEquals(true, shouldShow)
    }

    @Test
    fun `unselect visible action hides in selected only mode without filter`() {
        val shouldShow = shouldShowUnselectVisibleAction(
            selectedCount = 3,
            selectedVisibleCount = 3,
            showSelectedOnly = true,
            hasFilterQuery = false,
        )

        assertEquals(false, shouldShow)
    }

    @Test
    fun `unselect visible action shows in selected only filter mode with matches`() {
        val shouldShow = shouldShowUnselectVisibleAction(
            selectedCount = 3,
            selectedVisibleCount = 1,
            showSelectedOnly = true,
            hasFilterQuery = true,
        )

        assertEquals(true, shouldShow)
    }

    @Test
    fun `reference matched action shows for selected only filter subset`() {
        val shouldShow = shouldShowReferenceMatchedAction(
            selectedCount = 7,
            selectedVisibleCount = 3,
            showSelectedOnly = true,
            hasFilterQuery = true,
        )

        assertEquals(true, shouldShow)
    }

    @Test
    fun `reference matched action hides when every selected file matches`() {
        val shouldShow = shouldShowReferenceMatchedAction(
            selectedCount = 7,
            selectedVisibleCount = 7,
            showSelectedOnly = true,
            hasFilterQuery = true,
        )

        assertEquals(false, shouldShow)
    }

    @Test
    fun `reference matched action hides outside selected only filter mode`() {
        val shouldShow = shouldShowReferenceMatchedAction(
            selectedCount = 7,
            selectedVisibleCount = 3,
            showSelectedOnly = false,
            hasFilterQuery = true,
        )

        assertEquals(false, shouldShow)
    }

    @Test
    fun `file selection status label shows selected visible progress`() {
        val label = fileSelectionStatusLabel(
            selectedCount = 7,
            selectedVisibleCount = 3,
            visibleFileCount = 5,
            selectedSizeLabel = "12 KB",
        )

        assertEquals("已选 7 个 · 12 KB · 可见 3/5 · 隐藏 4", label)
    }

    @Test
    fun `file selection status label omits hidden count when all selected files are visible`() {
        val label = fileSelectionStatusLabel(
            selectedCount = 3,
            selectedVisibleCount = 3,
            visibleFileCount = 5,
            selectedSizeLabel = "12 KB",
        )

        assertEquals("已选 3 个 · 12 KB · 可见 3/5", label)
    }

    @Test
    fun `file selection status label shows selected match progress in selected only filter mode`() {
        val label = fileSelectionStatusLabel(
            selectedCount = 7,
            selectedVisibleCount = 3,
            visibleFileCount = 3,
            selectedSizeLabel = "12 KB",
            showSelectedOnly = true,
            hasFilterQuery = true,
        )

        assertEquals("已选 7 个 · 12 KB · 匹配 3/7", label)
    }

    @Test
    fun `file selection status label stays compact without selection`() {
        val label = fileSelectionStatusLabel(
            selectedCount = 0,
            selectedVisibleCount = 0,
            visibleFileCount = 5,
            selectedSizeLabel = "0 B",
        )

        assertEquals("可选 5 个", label)
    }

    @Test
    fun `file selection status label shows visible size without selection`() {
        val label = fileSelectionStatusLabel(
            selectedCount = 0,
            selectedVisibleCount = 0,
            visibleFileCount = 5,
            selectedSizeLabel = "0 B",
            visibleSizeLabel = "3 KB",
        )

        assertEquals("可选 5 个 · 3 KB", label)
    }
}
