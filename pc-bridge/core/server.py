# -*- coding: utf-8 -*-
"""WebSocket 服务端：鉴权、任务分发、流式回传。"""
import asyncio
import hmac
import logging
import socket
import uuid
from collections import OrderedDict

import websockets

from . import crypto, protocol
from .runners import CliTask, detect_cli_versions
from .terminal import TerminalSession


class _EncryptedWs:
    """透明加密包装：对底层 ws 收发的每条消息做 AES-GCM 解/加密。

    包装后交给 handle_client 的后续逻辑，业务代码无感知（同 _VirtualWs 思路）。
    """

    def __init__(self, ws, key: bytes):
        self._ws = ws
        self._key = key
        self.remote_address = getattr(ws, "remote_address", None)
        self.request = getattr(ws, "request", None)

    async def send(self, message: str):
        await self._ws.send(crypto.envelope(self._key, message))

    async def recv(self):
        import json as _json
        raw = await self._ws.recv()
        try:
            obj = _json.loads(raw)
            return crypto.decrypt(self._key, obj["enc"])
        except (ValueError, KeyError, TypeError) as exc:
            # 加密连接上收到非法/明文帧：当作连接异常结束，避免未捕获异常打挂 handler
            raise websockets.ConnectionClosed(None, None) from exc

    async def close(self, code: int = 1000, reason: str = ""):
        await self._ws.close(code=code, reason=reason)

    def __aiter__(self):
        return self

    async def __anext__(self):
        try:
            return await self.recv()
        except websockets.ConnectionClosed:
            raise StopAsyncIteration

log = logging.getLogger("nb-bridge")

AUTH_TIMEOUT_SEC = 10
RESULT_CACHE_SIZE = 100
AUTH_FAIL_LIMIT = 5          # 同一来源连续鉴权失败上限
AUTH_FAIL_WINDOW_SEC = 300   # 失败计数窗口/封禁时长
OUTPUT_BUFFER_MAX = 500      # 每任务输出缓存上限（断线增量补发用）


class BridgeServer:
    def __init__(self, config: dict):
        self.config = config
        self.cli_versions = detect_cli_versions(config["clis"])
        self.semaphore = asyncio.Semaphore(config.get("max_concurrent_tasks", 2))
        self.tasks = {}  # task_id -> CliTask
        self.task_streams = {}  # task_id -> 当前订阅输出的 ws（断线后可重挂）
        self.task_outputs = {}  # task_id -> [output payload...]（按 seq 顺序，断线重连可增量补发）
        self.results = OrderedDict()  # task_id -> 已完成任务的 done/error 消息（断线后取回）
        self.auth_failures = {}  # ip -> (失败次数, 首次失败时间)，防 token 爆破
        self.pending_permissions = {}  # task_id -> 发起审批的本地 MCP 连接
        self.terminals = {}  # term_id -> (TerminalSession, owner_ws)，远程终端会话
        self._write_mcp_config()

    def _write_mcp_config(self):
        """生成 claude --mcp-config 用的审批服务配置（每次启动刷新 python 路径）。"""
        import json as _json
        import sys as _sys
        from pathlib import Path as _Path
        root = _Path(__file__).resolve().parent.parent
        cfg = {"mcpServers": {"nbapproval": {
            "command": _sys.executable,
            "args": [str(root / "mcp_approval.py")],
        }}}
        with open(root / "nbapproval-mcp.json", "w", encoding="utf-8") as f:
            _json.dump(cfg, f, ensure_ascii=False, indent=2)
        # 审批模式用的隔离 settings：清空 allow 列表，确保每个工具都经手机审批
        settings = {"permissions": {"allow": [], "deny": [], "defaultMode": "default"}}
        with open(root / "approval-settings.json", "w", encoding="utf-8") as f:
            _json.dump(settings, f, ensure_ascii=False, indent=2)

    def _auth_blocked(self, ip: str) -> bool:
        import time as _time
        count, first = self.auth_failures.get(ip, (0, 0.0))
        if count >= AUTH_FAIL_LIMIT and _time.monotonic() - first < AUTH_FAIL_WINDOW_SEC:
            return True
        if _time.monotonic() - first >= AUTH_FAIL_WINDOW_SEC:
            self.auth_failures.pop(ip, None)
        return False

    def _record_auth_failure(self, ip: str):
        import time as _time
        count, first = self.auth_failures.get(ip, (0, _time.monotonic()))
        self.auth_failures[ip] = (count + 1, first)

    def _store_result(self, task_id: str, payload: str):
        self.results[task_id] = payload
        while len(self.results) > RESULT_CACHE_SIZE:
            self.results.popitem(last=False)

    async def serve_forever(self):
        host = self.config.get("host", "0.0.0.0")
        port = self.config.get("port", 7860)
        log.info("CLI 探测结果: %s", self.cli_versions)
        async with websockets.serve(self.handle_client, host, port, max_size=2 ** 22):
            log.info("nb-pc-bridge 监听 ws://%s:%d", host, port)
            await asyncio.Future()

    async def handle_client(self, ws):
        peer = ws.remote_address
        peer_ip = peer[0] if peer else "unknown"
        if self._auth_blocked(peer_ip):
            log.warning("鉴权封禁中，拒绝连接: %s", peer_ip)
            await ws.close(code=4029, reason="too many auth failures")
            return
        try:
            import json as _json
            raw = await asyncio.wait_for(ws.recv(), timeout=AUTH_TIMEOUT_SEC)
            # 首条可能是加密信封（无 type 字段），用宽松 JSON 解析，不强制 type
            msg = _json.loads(raw)
            if not isinstance(msg, dict):
                raise ValueError("首条消息必须是 JSON 对象")
        except (asyncio.TimeoutError, ValueError, websockets.ConnectionClosed):
            await ws.close(code=4001, reason="auth timeout")
            return

        # 加密握手：首条是 {"enc":...} 信封 → 用 token 派生密钥解密；
        # 解密成功本身即"知道 token"的证明（token 不上链），之后全程加密。
        if crypto.is_envelope(msg):
            key = crypto.derive_key(self.config["token"])
            try:
                inner = protocol.decode(crypto.decrypt(key, msg["enc"]))
            except Exception:
                self._record_auth_failure(peer_ip)
                log.warning("加密鉴权失败（密钥不符）: %s", peer)
                await ws.close(code=4003, reason="auth failed")
                return
            if inner.get("type") != "auth":
                await ws.close(code=4003, reason="auth failed")
                return
            self.auth_failures.pop(peer_ip, None)
            ws = _EncryptedWs(ws, key)
            await ws.send(protocol.auth_ok(socket.gethostname(), self.cli_versions))
            log.info("客户端已连接(加密): %s", peer)
        else:
            token = msg.get("token", "") if msg.get("type") == "auth" else ""
            if not hmac.compare_digest(token, self.config["token"]):
                self._record_auth_failure(peer_ip)
                log.warning("鉴权失败: %s", peer)
                await ws.send(protocol.auth_fail("token 不正确"))
                await ws.close(code=4003, reason="auth failed")
                return
            self.auth_failures.pop(peer_ip, None)
            await ws.send(protocol.auth_ok(socket.gethostname(), self.cli_versions))
            log.info("客户端已连接: %s", peer)

        try:
            async for raw in ws:
                try:
                    msg = protocol.decode(raw)
                except ValueError as exc:
                    await ws.send(protocol.error("", str(exc)))
                    continue
                await self.dispatch(ws, msg)
        except websockets.ConnectionClosed:
            pass
        finally:
            # 终端是交互式会话，连接断开即关闭其拥有的终端（不像后台任务保活）
            for tid in [t for t, (_, owner) in self.terminals.items() if owner is ws]:
                await self.close_terminal(tid)
            log.info("客户端断开: %s", peer)

    async def dispatch(self, ws, msg: dict):
        mtype = msg.get("type")
        if mtype == "ping":
            await ws.send(protocol.pong())
        elif mtype == "run":
            asyncio.ensure_future(self.run_task(ws, msg))
        elif mtype == "cancel":
            tid = msg.get("id", "")
            task = self.tasks.get(tid)
            if task is not None:
                await task.cancel()
                log.info("任务取消 id=%s", tid)
                await ws.send(protocol.cancelled(tid))
            else:
                await ws.send(protocol.error(tid, "任务不存在或已结束"))
        elif mtype == "get_result":
            await self.handle_get_result(ws, msg.get("id", ""), int(msg.get("sinceSeq") or 0))
        elif mtype == "permission_request":
            # 来自本地审批 MCP 服务：转发给任务当前订阅的手机连接
            tid = msg.get("id", "")
            phone_ws = self.task_streams.get(tid)
            if phone_ws is None:
                await ws.send(protocol.encode({
                    "type": "permission_response", "id": tid,
                    "approved": False, "message": "手机端不在线，自动拒绝",
                }))
            else:
                self.pending_permissions[tid] = ws
                log.info("权限请求转发到手机 id=%s tool=%s", tid, msg.get("toolName"))
                await phone_ws.send(protocol.encode(msg))
        elif mtype == "permission_response":
            # 来自手机：路由回等待中的审批 MCP 连接
            tid = msg.get("id", "")
            requester = self.pending_permissions.pop(tid, None)
            if requester is not None:
                log.info("权限响应 id=%s approved=%s", tid, msg.get("approved"))
                try:
                    await requester.send(protocol.encode(msg))
                except websockets.ConnectionClosed:
                    pass
        elif mtype == "term_open":
            await self.handle_term_open(ws, msg)
        elif mtype == "term_input":
            entry = self.terminals.get(msg.get("id", ""))
            if entry is not None:
                await entry[0].write(msg.get("data", ""))
        elif mtype == "term_resize":
            entry = self.terminals.get(msg.get("id", ""))
            if entry is not None:
                entry[0].resize(msg.get("cols", 0), msg.get("rows", 0))
        elif mtype == "term_close":
            await self.close_terminal(msg.get("id", ""))
        elif mtype == "list_dir":
            await self.handle_list_dir(ws, msg)
        elif mtype == "read_file":
            await self.handle_read_file(ws, msg)
        elif mtype == "write_file":
            await self.handle_write_file(ws, msg)
        elif mtype == "list_sessions":
            await self.handle_list_sessions(ws, msg)
        elif mtype == "list_tasks":
            running = [
                {
                    "id": tid,
                    "cli": t.cli,
                    "promptPreview": t.prompt[:80],
                    "cwd": t.cwd,
                    "elapsedMs": t.elapsed_ms(),
                }
                for tid, t in self.tasks.items()
            ]
            await ws.send(protocol.tasks(running))
        else:
            await ws.send(protocol.error(msg.get("id", ""), f"未知消息类型: {mtype}"))

    async def handle_term_open(self, ws, msg: dict):
        """开一个远程终端会话，输出流式回传发起的连接。"""
        term_id = msg.get("id") or uuid.uuid4().hex
        session = TerminalSession(
            term_id=term_id,
            cols=msg.get("cols", 0),
            rows=msg.get("rows", 0),
            cwd=(msg.get("cwd") or self.config.get("default_cwd") or None),
            cmd=(msg.get("cmd") or "").strip(),
        )
        self.terminals[term_id] = (session, ws)

        async def on_output(data: str):
            try:
                await ws.send(protocol.term_output(term_id, data))
            except websockets.ConnectionClosed:
                await self.close_terminal(term_id)

        async def on_exit(code: int):
            try:
                await ws.send(protocol.term_exit(term_id, code))
            except websockets.ConnectionClosed:
                pass
            self.terminals.pop(term_id, None)

        try:
            session.start(on_output, on_exit)
        except Exception as exc:
            self.terminals.pop(term_id, None)
            await ws.send(protocol.error(term_id, f"终端开启失败: {exc}"))
            log.warning("终端开启失败 id=%s: %s", term_id, exc)
            return
        await ws.send(protocol.accepted(term_id))
        log.info("终端会话建立 id=%s", term_id)

    async def close_terminal(self, term_id: str):
        entry = self.terminals.pop(term_id, None)
        if entry is not None:
            await entry[0].close()

    async def handle_list_dir(self, ws, msg: dict):
        from . import fileops
        req_id = msg.get("id", "")
        path = msg.get("path") or self.config.get("default_cwd", ".")
        try:
            listing = await asyncio.to_thread(fileops.list_dir, path)
            await ws.send(protocol.dir_listing(req_id, listing))
        except ValueError as exc:
            await ws.send(protocol.error(req_id, str(exc)))

    async def handle_read_file(self, ws, msg: dict):
        from . import fileops
        req_id = msg.get("id", "")
        path = msg.get("path", "")
        max_bytes = msg.get("maxBytes") or fileops.DEFAULT_MAX_BYTES
        try:
            result = await asyncio.to_thread(fileops.read_file, path, int(max_bytes))
            await ws.send(protocol.file_content(req_id, result))
        except ValueError as exc:
            await ws.send(protocol.error(req_id, str(exc)))

    async def handle_list_sessions(self, ws, msg: dict):
        from . import sessions
        req_id = msg.get("id", "")
        limit = int(msg.get("limit") or 20)
        cwd_filter = msg.get("cwd") or ""
        try:
            result = await asyncio.to_thread(sessions.list_sessions, limit, cwd_filter)
            await ws.send(protocol.sessions_list(req_id, result))
        except Exception as exc:
            await ws.send(protocol.error(req_id, f"列会话失败: {exc}"))

    async def handle_write_file(self, ws, msg: dict):
        from . import fileops
        req_id = msg.get("id", "")
        path = msg.get("path", "")
        content = msg.get("content", "")
        append = bool(msg.get("append"))
        try:
            result = await asyncio.to_thread(fileops.write_file, path, content, append)
            await ws.send(protocol.file_written(req_id, result))
            log.info("写文件 id=%s path=%s bytes=%d append=%s", req_id, result["path"], result["bytes"], append)
        except ValueError as exc:
            await ws.send(protocol.error(req_id, str(exc)))

    async def handle_get_result(self, ws, task_id: str, since_seq: int = 0):
        """断线重连后取回任务结果；任务仍在跑则先增量补发断线期间漏掉的输出，再重挂流。

        since_seq：手机已收到的最大 seq+1，桥接只补发 seq>=since_seq 的缓存输出，
        避免重复也不丢中间过程（历史增量同步）。
        """
        if task_id in self.results:
            # 任务已完成：先补发漏掉的输出，再发最终结果
            await self._replay_outputs(ws, task_id, since_seq)
            await ws.send(self.results[task_id])
            log.info("结果取回 id=%s（补发 since=%d）", task_id, since_seq)
        elif task_id in self.tasks:
            replayed = await self._replay_outputs(ws, task_id, since_seq)
            self.task_streams[task_id] = ws
            await ws.send(protocol.output(task_id, "status", f"任务执行中，已补发 {replayed} 条漏掉的输出并重新接上"))
            log.info("输出流重挂 id=%s（补发 %d 条 since=%d）", task_id, replayed, since_seq)
        else:
            await ws.send(protocol.error(task_id, "找不到这个任务，可能桥接服务重启过或任务太久之前"))

    async def _replay_outputs(self, ws, task_id: str, since_seq: int) -> int:
        """补发缓存里 seq>=since_seq 的输出，返回补发条数。"""
        buf = self.task_outputs.get(task_id, [])
        count = 0
        for seq, payload in buf:
            if seq >= since_seq:
                try:
                    await ws.send(payload)
                    count += 1
                except websockets.ConnectionClosed:
                    break
        return count

    async def run_task(self, ws, msg: dict):
        task_id = msg.get("id") or uuid.uuid4().hex
        cli = msg.get("cli", "")
        prompt = msg.get("prompt", "")
        if cli not in self.config["clis"]:
            await ws.send(protocol.error(task_id, f"不支持的 CLI: {cli}，可用: {list(self.config['clis'])}"))
            return
        if not prompt.strip():
            await ws.send(protocol.error(task_id, "prompt 不能为空"))
            return

        task = CliTask(
            task_id=task_id,
            cli=cli,
            prompt=prompt,
            cwd=msg.get("cwd") or self.config.get("default_cwd", "."),
            timeout_sec=int(msg.get("timeoutSec") or self.config.get("task_timeout_sec", 900)),
            cli_cfg=self.config["clis"][cli],
            resume_session_id=(msg.get("sessionId") or "").strip(),
            use_worktree=bool(msg.get("worktree")),
            approval_mode=(msg.get("approval") or "auto").strip() or "auto",
        )
        self.tasks[task_id] = task
        self.task_streams[task_id] = ws
        self.task_outputs[task_id] = []
        await ws.send(protocol.accepted(task_id))
        log.info("任务开始 id=%s cli=%s cwd=%s promptLen=%d", task_id, cli, task.cwd, len(prompt))

        async def send_to_stream(payload: str) -> bool:
            """发给当前订阅连接；客户端断线只断流不杀任务。"""
            stream = self.task_streams.get(task_id)
            if stream is None:
                return False
            try:
                await stream.send(payload)
                return True
            except websockets.ConnectionClosed:
                if self.task_streams.get(task_id) is stream:
                    self.task_streams.pop(task_id, None)
                    log.info("客户端断线，任务继续后台执行 id=%s", task_id)
                return False

        async def on_event(kind: str, text: str):
            # 带单调 seq 缓存每条输出，断线期间的输出重连后可增量补发，不丢中间过程
            buf = self.task_outputs.setdefault(task_id, [])
            seq = buf[-1][0] + 1 if buf else 0
            payload = protocol.output(task_id, kind, text, seq=seq)
            buf.append((seq, payload))
            if len(buf) > OUTPUT_BUFFER_MAX:
                del buf[0:len(buf) - OUTPUT_BUFFER_MAX]
            await send_to_stream(payload)

        try:
            async with self.semaphore:
                exit_code, result, duration_ms = await task.run(on_event)
            payload = protocol.done(task_id, exit_code, result, duration_ms,
                                    session_id=task.session_id,
                                    worktree_path=task.worktree_path,
                                    worktree_branch=task.worktree_branch)
            self._store_result(task_id, payload)
            await send_to_stream(payload)
            log.info("任务完成 id=%s exit=%d %dms", task_id, exit_code, duration_ms)
        except RuntimeError as exc:
            log.warning("任务失败 id=%s: %s", task_id, exc)
            payload = protocol.error(task_id, str(exc))
            self._store_result(task_id, payload)
            await send_to_stream(payload)
        finally:
            self.tasks.pop(task_id, None)
            self.task_streams.pop(task_id, None)
            self.task_outputs.pop(task_id, None)
            self.pending_permissions.pop(task_id, None)
