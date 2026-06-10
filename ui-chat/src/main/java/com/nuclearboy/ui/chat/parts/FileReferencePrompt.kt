package com.nuclearboy.ui.chat.parts

internal fun buildFileReferencePrompt(
    filePath: String,
    projectRoot: String,
): String {
    val relativePath = projectRelativeFilePath(filePath, projectRoot)
    return "请阅读项目文件 `$relativePath`，并结合我的要求继续处理："
}

internal fun buildFileReferencesPrompt(
    filePaths: List<String>,
    projectRoot: String,
): String {
    val relativePaths = filePaths
        .map { projectRelativeFilePath(it, projectRoot) }
        .distinct()
    return if (relativePaths.size == 1) {
        "请阅读项目文件 `${relativePaths.first()}`，并结合我的要求继续处理："
    } else {
        buildString {
            appendLine("请阅读以下项目文件，并结合我的要求继续处理：")
            relativePaths.forEach { path ->
                appendLine("- `$path`")
            }
        }.trimEnd()
    }
}

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

internal fun appendToChatDraft(
    currentDraft: String,
    addition: String,
): String {
    val current = currentDraft.trimEnd()
    return if (current.isBlank()) addition else "$current\n\n$addition"
}

internal fun projectRelativeFilePath(
    filePath: String,
    projectRoot: String,
): String {
    val normalizedRoot = projectRoot.toSlashPath().trimEnd('/')
    val normalizedPath = filePath.toSlashPath()
    return when {
        normalizedRoot.isNotBlank() && normalizedPath.startsWith("$normalizedRoot/") ->
            normalizedPath.removePrefix("$normalizedRoot/")
        normalizedPath == normalizedRoot -> "."
        else -> normalizedPath.substringAfterLast('/')
    }
}

private fun String.toSlashPath(): String = trim().replace('\\', '/')
