package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo

internal data class FilePanelTypeCount(
    val label: String,
    val count: Int,
)

internal data class FilePanelOverview(
    val directoryCount: Int,
    val fileCount: Int,
    val totalFileSizeBytes: Long,
    val typeCounts: List<FilePanelTypeCount>,
)

internal fun buildFilePanelOverview(
    files: List<FileInfo>,
    maxTypeCount: Int = 3,
): FilePanelOverview {
    val directories = files.count { it.isDirectory }
    val fileEntries = files.filterNot { it.isDirectory }
    val typeLimit = maxTypeCount.coerceAtLeast(0)
    val typeCounts = fileEntries
        .groupingBy { filePanelTypeLabel(it.extension) }
        .eachCount()
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key },
        )
        .take(typeLimit)
        .map { FilePanelTypeCount(label = it.key, count = it.value) }

    return FilePanelOverview(
        directoryCount = directories,
        fileCount = fileEntries.size,
        totalFileSizeBytes = fileEntries.sumOf { it.size },
        typeCounts = typeCounts,
    )
}

private fun filePanelTypeLabel(extension: String): String {
    val normalized = extension.trim().lowercase()
    return if (normalized.isEmpty()) "无扩展" else ".$normalized"
}
