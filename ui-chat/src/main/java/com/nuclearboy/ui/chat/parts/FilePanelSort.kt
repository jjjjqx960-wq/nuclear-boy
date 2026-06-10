package com.nuclearboy.ui.chat.parts

import com.nuclearboy.common.FileInfo

internal enum class FilePanelSortMode(
    val label: String,
) {
    Name("名称"),
    Type("类型"),
    Size("大小"),
    Recent("最近"),
}

internal fun sortFilePanelEntries(
    files: List<FileInfo>,
    mode: FilePanelSortMode,
): List<FileInfo> {
    val baseComparator = compareByDescending<FileInfo> { it.isDirectory }
    val modeComparator = when (mode) {
        FilePanelSortMode.Name -> compareBy<FileInfo> { it.name.lowercase() }
        FilePanelSortMode.Type -> compareBy<FileInfo> { it.extension.lowercase() }
            .thenBy { it.name.lowercase() }
        FilePanelSortMode.Size -> compareByDescending<FileInfo> { it.size }
            .thenBy { it.name.lowercase() }
        FilePanelSortMode.Recent -> compareByDescending<FileInfo> { it.lastModified }
            .thenBy { it.name.lowercase() }
    }
    return files.sortedWith(baseComparator.then(modeComparator))
}
