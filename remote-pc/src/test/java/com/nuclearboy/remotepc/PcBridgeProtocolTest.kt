package com.nuclearboy.remotepc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcBridgeProtocolTest {

    @Test
    fun `encodeAuth produces auth message with token`() {
        val raw = PcBridgeProtocol.encodeAuth("secret-token")
        assertTrue(raw.contains("\"type\":\"auth\""))
        assertTrue(raw.contains("\"token\":\"secret-token\""))
    }

    @Test
    fun `encodeRun omits null cwd and timeout`() {
        val raw = PcBridgeProtocol.encodeRun(
            PcBridgeProtocol.RunMessage(id = "t1", cli = "claude", prompt = "你好")
        )
        assertTrue(raw.contains("\"cli\":\"claude\""))
        assertTrue(!raw.contains("cwd"))
        assertTrue(!raw.contains("timeoutSec"))
    }

    @Test
    fun `parse auth_ok with clis map`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"auth_ok","host":"MY-PC","clis":{"claude":"2.1.175","codex":"0.139.0"}}"""
        )
        val authOk = msg as PcBridgeProtocol.Inbound.AuthOk
        assertEquals("MY-PC", authOk.host)
        assertEquals("2.1.175", authOk.clis["claude"])
        assertEquals(2, authOk.clis.size)
    }

    @Test
    fun `parse output event`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"output","id":"t1","kind":"text","text":"hello"}"""
        )
        val output = msg as PcBridgeProtocol.Inbound.Output
        assertEquals("t1", output.id)
        assertEquals("text", output.kind)
        assertEquals("hello", output.text)
    }

    @Test
    fun `parse done with numeric fields`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"done","id":"t1","exitCode":0,"result":"2","durationMs":8719}"""
        )
        val done = msg as PcBridgeProtocol.Inbound.Done
        assertEquals(0, done.exitCode)
        assertEquals("2", done.result)
        assertEquals(8719L, done.durationMs)
    }

    @Test
    fun `parse error and auth_fail`() {
        val err = PcBridgeProtocol.parseInbound("""{"type":"error","id":"t1","message":"任务超时"}""")
        assertEquals("任务超时", (err as PcBridgeProtocol.Inbound.Error).message)

        val fail = PcBridgeProtocol.parseInbound("""{"type":"auth_fail","message":"token 不正确"}""")
        assertEquals("token 不正确", (fail as PcBridgeProtocol.Inbound.AuthFail).message)
    }

    @Test
    fun `parse pong and unknown type`() {
        assertEquals(PcBridgeProtocol.Inbound.Pong, PcBridgeProtocol.parseInbound("""{"type":"pong"}"""))
        val unknown = PcBridgeProtocol.parseInbound("""{"type":"future_event"}""")
        assertEquals("future_event", (unknown as PcBridgeProtocol.Inbound.Unknown).type)
    }

    @Test
    fun `parse invalid json returns null`() {
        assertNull(PcBridgeProtocol.parseInbound("not json"))
        assertNull(PcBridgeProtocol.parseInbound("""{"noType":1}"""))
        assertNull(PcBridgeProtocol.parseInbound("[1,2,3]"))
    }

    @Test
    fun `parse done with missing fields falls back to defaults`() {
        val done = PcBridgeProtocol.parseInbound("""{"type":"done","id":"t1"}""")
            as PcBridgeProtocol.Inbound.Done
        assertEquals(-1, done.exitCode)
        assertEquals("", done.result)
        assertEquals(0L, done.durationMs)
    }
}
