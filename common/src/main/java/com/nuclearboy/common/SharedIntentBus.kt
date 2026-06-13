package com.nuclearboy.common

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 外部分享进来的文本（Android 分享菜单 → 核弹男孩）。
 *
 * MainActivity 收到 ACTION_SEND 时 emit，聊天界面收集后回填输入框让用户补充再发。
 * replay=1：冷启动时分享先到、界面后订阅也能拿到；DROP_OLDEST 避免堆积。
 */
object SharedIntentBus {

    private val _shared = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val shared: SharedFlow<String> = _shared.asSharedFlow()

    /** 投递一段分享文本（空白忽略）。 */
    fun emit(text: String) {
        val t = text.trim()
        if (t.isNotEmpty()) _shared.tryEmit(t)
    }
}
