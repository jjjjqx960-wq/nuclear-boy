package com.nuclearboy.ui.chat.parts

internal fun buildFileReferencePrompt(
    filePath: String,
    projectRoot: String,
): String {
    val relativePath = projectRelativeFilePath(filePath, projectRoot)
    return "请阅读项目文件 `$relativePath`，并结合我的要求继续处理："
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
