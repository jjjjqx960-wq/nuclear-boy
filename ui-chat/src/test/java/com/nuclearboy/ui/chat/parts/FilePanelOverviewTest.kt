package com.nuclearboy.ui.chat.parts

import org.junit.Assert.assertEquals
import org.junit.Test

class FilePanelOverviewTest {
    @Test
    fun `overview pill description includes label and value`() {
        val description = filePanelOverviewPillDescription(
            label = "文件",
            value = "12",
        )

        assertEquals("文件 12", description)
    }

    @Test
    fun `overview pill description trims label and value`() {
        val description = filePanelOverviewPillDescription(
            label = " .kt ",
            value = " 5 ",
        )

        assertEquals(".kt 5", description)
    }

    @Test
    fun `overview pill description falls back to label without value`() {
        val description = filePanelOverviewPillDescription(
            label = "大小",
            value = " ",
        )

        assertEquals("大小", description)
    }

    @Test
    fun `overview pill description falls back to summary without label or value`() {
        val description = filePanelOverviewPillDescription(
            label = " ",
            value = " ",
        )

        assertEquals("摘要", description)
    }
}
