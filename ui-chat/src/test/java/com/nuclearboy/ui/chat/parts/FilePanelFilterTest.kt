package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FilePanelFilterTest {
    private val files = listOf(
        FileInfo(path = "app/src/Main.kt", name = "Main.kt", extension = "kt"),
        FileInfo(path = "README.md", name = "README.md", extension = "md"),
        FileInfo(path = "gradle/libs.versions.toml", name = "libs.versions.toml", extension = "toml"),
    )

    @Test
    fun `file panel filter matches name extension and path`() {
        val kotlinFiles = filterFilePanelEntries(files, "kt")
        val gradlePathFiles = filterFilePanelEntries(files, "gradle/")

        assertEquals(listOf("Main.kt"), kotlinFiles.map { it.name })
        assertEquals(listOf("libs.versions.toml"), gradlePathFiles.map { it.name })
    }

    @Test
    fun `visible file panel entries filters selected files in selected only mode`() {
        val visibleFiles = visibleFilePanelEntries(
            filteredFiles = files,
            selectedFiles = listOf(files[0], files[1]),
            showSelectedOnly = true,
            query = "read",
        )

        assertEquals(listOf("README.md"), visibleFiles.map { it.name })
    }

    @Test
    fun `visible file panel entries uses filtered files outside selected only mode`() {
        val visibleFiles = visibleFilePanelEntries(
            filteredFiles = listOf(files[2]),
            selectedFiles = listOf(files[0], files[1]),
            showSelectedOnly = false,
            query = "read",
        )

        assertEquals(listOf("libs.versions.toml"), visibleFiles.map { it.name })
    }

    @Test
    fun `file panel filter summary uses filtered result count`() {
        val summary = filePanelFilterSummary(
            totalCount = files.size,
            filteredCount = 1,
            query = "kt",
        )

        assertEquals("1 / 3", summary)
    }

    @Test
    fun `file panel filter summary shows total count without query`() {
        val summary = filePanelFilterSummary(
            totalCount = files.size,
            filteredCount = 1,
            query = " ",
        )

        assertEquals("3 项", summary)
    }

    @Test
    fun `file panel clear filter description includes result summary`() {
        val description = filePanelClearFilterDescription("1 / 3")

        assertEquals("清除过滤，当前 1 / 3", description)
    }

    @Test
    fun `file panel clear filter description stays compact without summary`() {
        val description = filePanelClearFilterDescription(" ")

        assertEquals("清除过滤", description)
    }
}
