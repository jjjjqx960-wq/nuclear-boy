package com.nuclearboy.common

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedIntentBusTest {

    @Test
    fun `emitted text is replayed to a later collector`() = runTest {
        SharedIntentBus.emit("分享进来的文本")
        // replay=1：后订阅也能拿到最近一条
        assertEquals("分享进来的文本", SharedIntentBus.shared.first())
    }

    @Test
    fun `blank text is ignored`() = runTest {
        SharedIntentBus.emit("有效内容")
        SharedIntentBus.emit("   ")  // 空白不应覆盖
        assertEquals("有效内容", SharedIntentBus.shared.first())
    }

    @Test
    fun `trims surrounding whitespace`() = runTest {
        SharedIntentBus.emit("  带空格  ")
        assertEquals("带空格", SharedIntentBus.shared.first())
    }
}
