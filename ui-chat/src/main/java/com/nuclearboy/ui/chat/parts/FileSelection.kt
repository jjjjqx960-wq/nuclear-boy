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
    referencedSizeLabel: String = "",
): String {
    val sizeSuffix = if (referencedSizeLabel.isNotBlank()) " · $referencedSizeLabel" else ""
    val referencedLabel = "已引用 $referencedCount 个文件$sizeSuffix"
    return if (remainingSelectedCount > 0) {
        "$referencedLabel，剩余 $remainingSelectedCount 个已选"
    } else {
        referencedLabel
    }
}

internal fun fileReferenceToastMessage(
    fileName: String,
    fileSizeLabel: String = "",
): String {
    val safeFileName = fileName.ifBlank { "文件" }
    val sizeSuffix = if (fileSizeLabel.isNotBlank()) " · $fileSizeLabel" else ""
    return "已引用: $safeFileName$sizeSuffix"
}

internal fun shouldClosePanelAfterMatchedReference(
    remainingSelectedCount: Int,
): Boolean {
    return remainingSelectedCount <= 0
}

internal fun fileSelectionReferenceMatchedActionLabel(
    matchedCount: Int,
): String {
    return "匹配 ${matchedCount.coerceAtLeast(0)}"
}

internal fun fileSelectionReferenceSelectedActionLabel(
    selectedCount: Int,
    hasMatchedAction: Boolean,
): String {
    val action = if (hasMatchedAction) "全部" else "引用"
    val count = selectedCount.coerceAtLeast(0)
    return if (count > 0) "$action $count" else action
}

internal fun fileSelectionReferenceVisibleActionLabel(
    visibleFileCount: Int,
): String {
    val count = visibleFileCount.coerceAtLeast(0)
    return if (count > 0) "引用 $count" else "引用"
}

internal fun fileSelectionReferenceVisibleActionDescription(
    visibleFileCount: Int,
): String {
    return "引用当前可见 ${visibleFileCount.coerceAtLeast(0)} 个文件到输入"
}

internal fun fileSelectionSelectVisibleActionLabel(
    selectedVisibleCount: Int,
    visibleFileCount: Int,
): String {
    val safeSelectedVisibleCount = selectedVisibleCount.coerceAtLeast(0)
    val safeVisibleFileCount = visibleFileCount.coerceAtLeast(0)
    val addableCount = (safeVisibleFileCount - safeSelectedVisibleCount).coerceAtLeast(0)
    val action = if (safeSelectedVisibleCount > 0) "补选" else "全选"
    return "$action $addableCount"
}

internal fun fileSelectionSelectedOnlyToggleLabel(
    showSelectedOnly: Boolean,
    selectedCount: Int,
    selectedVisibleCount: Int,
    allVisibleFileCount: Int,
    hasFilterQuery: Boolean,
): String {
    return if (showSelectedOnly) {
        "全部 ${allVisibleFileCount.coerceAtLeast(0)}"
    } else {
        val targetCount = if (hasFilterQuery) selectedVisibleCount else selectedCount
        "只看 ${targetCount.coerceAtLeast(0)}"
    }
}

internal fun fileSelectionClearActionDescription(
    selectedCount: Int,
): String {
    val count = selectedCount.coerceAtLeast(0)
    return if (count > 0) "清空 $count 个已选文件" else "清空选择"
}

internal fun fileSelectionUnselectVisibleActionDescription(
    selectedVisibleCount: Int,
    showSelectedOnly: Boolean,
): String {
    val count = selectedVisibleCount.coerceAtLeast(0)
    return if (showSelectedOnly) {
        "取消匹配 $count 个已选文件"
    } else {
        "取消当前可见 $count 个已选文件"
    }
}

internal fun fileSelectionUnselectHiddenActionDescription(
    hiddenSelectedCount: Int,
): String {
    return "取消隐藏 ${hiddenSelectedCount.coerceAtLeast(0)} 个已选文件"
}

internal fun fileSelectionTotalSizeBytes(files: List<FileInfo>): Long {
    return files
        .filterNot { it.isDirectory }
        .sumOf { it.size.coerceAtLeast(0L) }
}

internal fun selectedFileTotalSizeBytes(files: List<FileInfo>): Long {
    return fileSelectionTotalSizeBytes(files)
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
    visibleSizeLabel: String = "",
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
        val visibleSizeSuffix = if (visibleSizeLabel.isNotBlank()) " · $visibleSizeLabel" else ""
        "可选 $visibleFileCount 个$visibleSizeSuffix"
    }
}

internal fun selectedFilePanelEntries(
    files: List<FileInfo>,
    selectedPaths: List<String>,
): List<FileInfo> {
    val selectedPathSet = selectedPaths.toSet()
    return files.filter { !it.isDirectory && it.path in selectedPathSet }
}
