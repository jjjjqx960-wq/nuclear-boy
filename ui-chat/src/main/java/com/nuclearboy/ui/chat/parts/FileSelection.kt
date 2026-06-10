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
