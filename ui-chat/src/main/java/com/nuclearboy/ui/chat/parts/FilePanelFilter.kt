package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo

internal fun filterFilePanelEntries(
    files: List<FileInfo>,
    query: String,
): List<FileInfo> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isEmpty()) return files

    return files.filter { file ->
        file.name.contains(normalizedQuery, ignoreCase = true) ||
            file.extension.contains(normalizedQuery, ignoreCase = true) ||
            file.path.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun visibleFilePanelEntries(
    filteredFiles: List<FileInfo>,
    selectedFiles: List<FileInfo>,
    showSelectedOnly: Boolean,
    query: String,
): List<FileInfo> {
    return if (showSelectedOnly) {
        filterFilePanelEntries(selectedFiles, query)
    } else {
        filteredFiles
    }
}

internal fun filePanelFilterSummary(
    totalCount: Int,
    filteredCount: Int,
    query: String,
): String {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) {
        "$totalCount 项"
    } else {
        "$filteredCount / $totalCount"
    }
}
