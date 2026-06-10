package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FilePanelSortTest {
    private val files = listOf(
        FileInfo(path = "src", name = "src", isDirectory = true, lastModified = 20L),
        FileInfo(path = "README.md", name = "README.md", extension = "md", size = 200L, lastModified = 30L),
        FileInfo(path = "app/Main.kt", name = "Main.kt", extension = "kt", size = 500L, lastModified = 10L),
        FileInfo(path = "build.gradle", name = "build.gradle", extension = "gradle", size = 100L, lastModified = 40L),
    )

    @Test
    fun `name sort keeps directories first and sorts entries by name`() {
        val sorted = sortFilePanelEntries(files, FilePanelSortMode.Name)

        assertEquals(
            listOf("src", "build.gradle", "Main.kt", "README.md"),
            sorted.map { it.name },
        )
    }

    @Test
    fun `type sort keeps directories first and sorts files by extension`() {
        val sorted = sortFilePanelEntries(files, FilePanelSortMode.Type)

        assertEquals(
            listOf("src", "build.gradle", "Main.kt", "README.md"),
            sorted.map { it.name },
        )
    }

    @Test
    fun `size sort keeps directories first and sorts files largest first`() {
        val sorted = sortFilePanelEntries(files, FilePanelSortMode.Size)

        assertEquals(
            listOf("src", "Main.kt", "README.md", "build.gradle"),
            sorted.map { it.name },
        )
    }

    @Test
    fun `recent sort keeps directories first and sorts files newest first`() {
        val sorted = sortFilePanelEntries(files, FilePanelSortMode.Recent)

        assertEquals(
            listOf("src", "build.gradle", "README.md", "Main.kt"),
            sorted.map { it.name },
        )
    }
}
