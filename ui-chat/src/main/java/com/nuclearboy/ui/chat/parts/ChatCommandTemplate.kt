package com.nuclearboy.ui.chat.parts

internal enum class ChatCommandKind {
    Goal,
    Loop,
    Compact,
    Rewind,
    Model,
    Stop,
}

internal data class ChatCommandTemplate(
    val kind: ChatCommandKind,
    val label: String,
    val commandText: String,
    val submitImmediately: Boolean,
)

internal fun chatCommandTemplates(
    isProcessing: Boolean,
    hasMessages: Boolean,
): List<ChatCommandTemplate> {
    if (isProcessing) {
        return listOf(
            ChatCommandTemplate(
                kind = ChatCommandKind.Stop,
                label = "停止",
                commandText = "/stop",
                submitImmediately = true,
            ),
        )
    }

    return buildList {
        add(
            ChatCommandTemplate(
                kind = ChatCommandKind.Goal,
                label = "目标",
                commandText = "/goal ",
                submitImmediately = false,
            ),
        )
        add(
            ChatCommandTemplate(
                kind = ChatCommandKind.Loop,
                label = "循环",
                commandText = "/loop 5 ",
                submitImmediately = false,
            ),
        )
        if (hasMessages) {
            add(
                ChatCommandTemplate(
                    kind = ChatCommandKind.Compact,
                    label = "压缩",
                    commandText = "/compact",
                    submitImmediately = false,
                ),
            )
            add(
                ChatCommandTemplate(
                    kind = ChatCommandKind.Rewind,
                    label = "回退",
                    commandText = "/rewind 1",
                    submitImmediately = false,
                ),
            )
        }
        add(
            ChatCommandTemplate(
                kind = ChatCommandKind.Model,
                label = "模型",
                commandText = "/model",
                submitImmediately = true,
            ),
        )
    }
}
