package com.nuclearboy.common

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 外部触发的一次性导航指令（如桌面长按图标的快捷方式）。
 *
 * MainActivity 收到带 nb_nav 的 intent 时 emit 目标，根 Compose 收集后导航过去。
 * replay=1：冷启动时指令先到、界面后订阅也能拿到。
 */
object NavCommandBus {

    /** 已知目标常量（与 NavRoutes 对应）。 */
    const val TARGET_TERMINAL = "terminal"

    private val _commands = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val commands: SharedFlow<String> = _commands.asSharedFlow()

    fun emit(target: String) {
        val t = target.trim()
        if (t.isNotEmpty()) _commands.tryEmit(t)
    }
}
