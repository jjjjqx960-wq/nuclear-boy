package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo

internal fun toggleSelectedFilePath(
    selectedPaths: List<String>,
    filePath: String,
): List<String> {
    return if (filePath in selectedPaths) {
        selectedPaths.filterNot { it == filePath }
    } else {
        selectedPaths + filePath
    }
}

internal fun selectVisibleFilePaths(
    selectedPaths: List<String>,
    visibleFiles: List<FileInfo>,
): List<String> {
    val visibleFilePaths = visibleFiles
        .filterNot { it.isDirectory }
        .map { it.path }
    return (selectedPaths + visibleFilePaths).distinct()
}

internal fun unselectVisibleFilePaths(
    selectedPaths: List<String>,
    visibleFiles: List<FileInfo>,
): List<String> {
    val visibleFilePathSet = visibleFiles
        .filterNot { it.isDirectory }
        .map { it.path }
        .toSet()
    return selectedPaths.filterNot { it in visibleFilePathSet }
}

internal fun unselectHiddenFilePaths(
    selectedPaths: List<String>,
    visibleFiles: List<FileInfo>,
): List<String> {
    val visibleFilePathSet = visibleFiles
        .filterNot { it.isDirectory }
        .map { it.path }
        .toSet()
    return selectedPaths.filter { it in visibleFilePathSet }
}

internal fun removeReferencedFilePaths(
    selectedPaths: List<String>,
    referencedFiles: List<FileInfo>,
): List<String> {
    val referencedFilePathSet = referencedFiles
        .filterNot { it.isDirectory }
        .map { it.path }
        .toSet()
    return selectedPaths.filterNot { it in referencedFilePathSet }
}

internal fun filterQueryAfterMatchedReference(
    remainingSelectedCount: Int,
    currentQuery: String,
): String {
    return if (remainingSelectedCount > 0 && currentQuery.isNotBlank()) "" else currentQuery
}

internal fun fileReferencesToastMessage(
    referencedCount: Int,
    remainingSelectedCount: Int,
): String {
    return if (remainingSelectedCount > 0) {
        "已引用 $referencedCount 个文件，剩余 $remainingSelectedCount 个已选"
    } else {
        "已引用 $referencedCount 个文件"
    }
}

internal fun selectedFileTotalSizeBytes(files: List<FileInfo>): Long {
    return files
        .filterNot { it.isDirectory }
        .sumOf { it.size.coerceAtLeast(0L) }
}

internal fun shouldShowFileSelectionActionBar(
    selectedCount: Int,
    visibleFileCount: Int,
): Boolean {
    return selectedCount > 0 || visibleFileCount > 0
}

internal fun shouldShowUnselectVisibleAction(
    selectedCount: Int,
    selectedVisibleCount: Int,
    showSelectedOnly: Boolean,
    hasFilterQuery: Boolean,
): Boolean {
    return selectedCount > 0 &&
        selectedVisibleCount > 0 &&
        (!showSelectedOnly || hasFilterQuery)
}

internal fun shouldShowReferenceMatchedAction(
    selectedCount: Int,
    selectedVisibleCount: Int,
    showSelectedOnly: Boolean,
    hasFilterQuery: Boolean,
): Boolean {
    return selectedCount > 0 &&
        selectedVisibleCount > 0 &&
        selectedVisibleCount < selectedCount &&
        showSelectedOnly &&
        hasFilterQuery
}

internal fun fileSelectionStatusLabel(
    selectedCount: Int,
    selectedVisibleCount: Int,
    visibleFileCount: Int,
    selectedSizeLabel: String,
    showSelectedOnly: Boolean = false,
    hasFilterQuery: Boolean = false,
): String {
    return if (selectedCount > 0) {
        if (showSelectedOnly && hasFilterQuery) {
            "已选 $selectedCount 个 · $selectedSizeLabel · 匹配 $selectedVisibleCount/$selectedCount"
        } else {
            val hiddenSelectedCount = (selectedCount - selectedVisibleCount).coerceAtLeast(0)
            val hiddenLabel = if (hiddenSelectedCount > 0) " · 隐藏 $hiddenSelectedCount" else ""
            "已选 $selectedCount 个 · $selectedSizeLabel · 可见 $selectedVisibleCount/$visibleFileCount$hiddenLabel"
        }
    } else {
        "可选 $visibleFileCount 个"
    }
}

internal fun selectedFilePanelEntries(
    files: List<FileInfo>,
    selectedPaths: List<String>,
): List<FileInfo> {
    val selectedPathSet = selectedPaths.toSet()
    return files.filter { !it.isDirectory && it.path in selectedPathSet }
}
