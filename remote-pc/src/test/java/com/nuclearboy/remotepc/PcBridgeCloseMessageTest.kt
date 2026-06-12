package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcBridgeCloseMessageTest {

    @Test
    fun `relay key invalid maps to key hint`() {
        val msg = PcBridgeClient.friendlyCloseMessage(4003, "relay key invalid")
        assertTrue(msg.contains("口令"))
        assertTrue(!msg.contains("relay key invalid"))  // 不再泄露英文原文
    }

    @Test
    fun `no agent online maps to PC offline hint`() {
        val msg = PcBridgeClient.friendlyCloseMessage(4004, "no agent online in this room")
        assertTrue(msg.contains("电脑还没上线"))
    }

    @Test
    fun `room taken maps to room hint`() {
        assertTrue(PcBridgeClient.friendlyCloseMessage(4009, "room already has an agent").contains("房间"))
    }

    @Test
    fun `auth ban maps to retry-later hint`() {
        assertTrue(PcBridgeClient.friendlyCloseMessage(4029, "").contains("封禁"))
    }

    @Test
    fun `unknown code falls back to reason text`() {
        assertEquals("自定义原因", PcBridgeClient.friendlyCloseMessage(1011, "自定义原因"))
    }

    @Test
    fun `unknown code with blank reason falls back to generic`() {
        assertEquals("连接已关闭（1006）", PcBridgeClient.friendlyCloseMessage(1006, ""))
    }
}
