package com.nuclearboy.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 工具执行进度总线。
 *
 * 长耗时工具（如 pc_cli_run 远程任务）在执行中通过它上报增量进度，
 * 聊天界面订阅后实时更新对应工具卡片，用户不用干等结果。
 *
 * 设计约束：Agent 引擎串行执行工具，同名工具同一时刻只有一个在跑，
 * 因此事件只带 toolName 不带 toolCallId（executor 拿不到 id）。
 */
object ToolProgressBus {

    data class ToolProgress(
        val toolName: String,
        val text: String,
    )

    private val _events = MutableSharedFlow<ToolProgress>(
        extraBufferCapacity = 64,
    )
    val events: SharedFlow<ToolProgress> = _events.asSharedFlow()

    /** 非挂起上报：缓冲满了直接丢弃旧进度（进度展示允许丢行，不能阻塞执行）。 */
    fun report(toolName: String, text: String) {
        if (text.isBlank()) return
        _events.tryEmit(ToolProgress(toolName, text.take(MAX_LINE_LENGTH)))
    }

    private const val MAX_LINE_LENGTH = 300
}
