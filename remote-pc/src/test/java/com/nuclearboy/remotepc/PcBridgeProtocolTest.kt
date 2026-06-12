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
        assertEquals("", done.sessionId)
    }

    @Test
    fun `parse done with session id for resume`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"done","id":"t1","exitCode":0,"result":"已记住","durationMs":8780,"sessionId":"f632d877-1060-4663-8782-b5115a77b1e7"}"""
        )
        val done = msg as PcBridgeProtocol.Inbound.Done
        assertEquals("f632d877-1060-4663-8782-b5115a77b1e7", done.sessionId)
    }

    @Test
    fun `encodeGetResult produces get_result message`() {
        val raw = PcBridgeProtocol.encodeGetResult("task-1")
        assertTrue(raw.contains("\"type\":\"get_result\""))
        assertTrue(raw.contains("\"id\":\"task-1\""))
    }

    @Test
    fun `encodeRun includes session id when resuming`() {
        val raw = PcBridgeProtocol.encodeRun(
            PcBridgeProtocol.RunMessage(id = "t2", cli = "claude", prompt = "继续", sessionId = "abc-123")
        )
        assertTrue(raw.contains("\"sessionId\":\"abc-123\""))
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
    fun `parse tasks list with running entries`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"tasks","tasks":[{"id":"t1","cli":"claude","promptPreview":"修 bug","cwd":"D:\\proj","elapsedMs":3007}]}"""
        )
        val tasks = (msg as PcBridgeProtocol.Inbound.Tasks).tasks
        assertEquals(1, tasks.size)
        assertEquals("claude", tasks[0].cli)
        assertEquals(3007L, tasks[0].elapsedMs)
    }

    @Test
    fun `parse empty tasks list`() {
        val msg = PcBridgeProtocol.parseInbound("""{"type":"tasks","tasks":[]}""")
        assertTrue((msg as PcBridgeProtocol.Inbound.Tasks).tasks.isEmpty())
    }

    @Test
    fun `parse cancelled confirmation`() {
        val msg = PcBridgeProtocol.parseInbound("""{"type":"cancelled","id":"t9"}""")
        assertEquals("t9", (msg as PcBridgeProtocol.Inbound.Cancelled).id)
    }

    @Test
    fun `encodeListTasks produces list_tasks message`() {
        assertTrue(PcBridgeProtocol.encodeListTasks().contains("\"type\":\"list_tasks\""))
    }

    @Test
    fun `parse done with worktree info`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"done","id":"t1","exitCode":0,"result":"done","durationMs":100,"worktreePath":"D:/x/.nb-worktrees/repo-ab","worktreeBranch":"nb/ab"}"""
        )
        val done = msg as PcBridgeProtocol.Inbound.Done
        assertTrue(done.worktreePath.contains(".nb-worktrees"))
        assertEquals("nb/ab", done.worktreeBranch)
    }

    @Test
    fun `encodeRun includes worktree flag when isolating`() {
        val raw = PcBridgeProtocol.encodeRun(
            PcBridgeProtocol.RunMessage(id = "t3", cli = "claude", prompt = "改", worktree = true)
        )
        assertTrue(raw.contains("\"worktree\":true"))
    }

    @Test
    fun `parse permission_request with input summary`() {
        val msg = PcBridgeProtocol.parseInbound(
            """{"type":"permission_request","id":"t1","toolName":"Bash","input":{"command":"rm -rf x"}}"""
        )
        val req = msg as PcBridgeProtocol.Inbound.PermissionRequest
        assertEquals("Bash", req.toolName)
        assertTrue(req.inputSummary.contains("command"))
    }

    @Test
    fun `encodePermissionResponse carries decision`() {
        val raw = PcBridgeProtocol.encodePermissionResponse("t1", approved = true)
        assertTrue(raw.contains("\"type\":\"permission_response\""))
        assertTrue(raw.contains("\"approved\":true"))
    }

    @Test
    fun `encodeRun includes approval mode`() {
        val raw = PcBridgeProtocol.encodeRun(
            PcBridgeProtocol.RunMessage(id = "t4", cli = "claude", prompt = "x", approval = "ask")
        )
        assertTrue(raw.contains("\"approval\":\"ask\""))
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
