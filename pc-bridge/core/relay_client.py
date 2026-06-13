# -*- coding: utf-8 -*-
"""电脑端反连公网中继：把单条中继 agent 连接 demux 成多个虚拟连接。

每个手机（cid）对应一个 _VirtualWs，喂给 BridgeServer.handle_client，
完全复用本地直连的鉴权/任务/审批逻辑，业务代码零改动。
中继断线自动重连。
"""
import asyncio
import json
import logging

import websockets

log = logging.getLogger("nb-bridge")

RECONNECT_DELAY_SEC = 5


class _VirtualWs:
    """伪装成 server 端 WebSocket 的虚拟连接，对应中继里的一个 cid。

    只实现 handle_client 用到的接口：recv / send / close / __aiter__ /
    request.path / remote_address。
    """

    class _Req:
        path = "/"

    def __init__(self, cid: str, relay_ws, room: str):
        self.cid = cid
        self._relay_ws = relay_ws
        self.request = self._Req()
        self.remote_address = (f"relay:{room}:{cid}", 0)
        self._inbox = asyncio.Queue()
        self._closed = False

    async def deliver(self, data: str):
        await self._inbox.put(data)

    def mark_closed(self):
        self._closed = True
        self._inbox.put_nowait(None)  # 唤醒 recv

    async def recv(self):
        data = await self._inbox.get()
        if data is None:
            raise websockets.ConnectionClosed(None, None)
        return data

    async def send(self, message: str):
        if self._closed:
            return
        await self._relay_ws.send(json.dumps({"_cid": self.cid, "_data": message}))

    async def close(self, code: int = 1000, reason: str = ""):
        self._closed = True
        # 把 server 主动关闭（如鉴权封禁 4029）透传给中继，让它带码关掉对应手机，
        # 否则手机收不到关闭码只能干等超时。
        try:
            await self._relay_ws.send(json.dumps({
                "_cid": self.cid, "_event": "server_close",
                "_code": code, "_reason": reason,
            }))
        except Exception:
            pass

    def __aiter__(self):
        return self

    async def __anext__(self):
        try:
            return await self.recv()
        except websockets.ConnectionClosed:
            raise StopAsyncIteration


async def run_relay_agent(server, relay_url: str, room: str, relay_key: str):
    """连接中继 agent 端点并持续服务，断线自动重连。"""
    sep = "&" if "?" in relay_url else "?"
    agent_url = f"{relay_url.rstrip('/')}/agent/{room}"
    if relay_key:
        agent_url += f"{sep}key={relay_key}"

    while True:
        try:
            log.info("反连中继 %s room=%s", relay_url, room)
            async with websockets.connect(agent_url, max_size=2 ** 22) as relay_ws:
                log.info("已挂载到中继，等待手机连接")
                await _serve_relay(server, relay_ws, room)
        except (websockets.ConnectionClosed, OSError) as exc:
            log.warning("中继连接断开: %s，%ds 后重连", exc, RECONNECT_DELAY_SEC)
        except Exception as exc:
            log.warning("中继异常: %s，%ds 后重连", exc, RECONNECT_DELAY_SEC)
        await asyncio.sleep(RECONNECT_DELAY_SEC)


async def _serve_relay(server, relay_ws, room: str):
    virtuals = {}  # cid -> (_VirtualWs, task)
    try:
        async for raw in relay_ws:
            try:
                env = json.loads(raw)
            except json.JSONDecodeError:
                continue
            cid = env.get("_cid")
            if cid is None:
                continue
            if env.get("_event") == "close":
                pair = virtuals.pop(cid, None)
                if pair is not None:
                    pair[0].mark_closed()
                continue
            data = env.get("_data")
            if data is None:
                continue
            pair = virtuals.get(cid)
            if pair is None:
                # 新手机会话：建虚拟连接并启动 handle_client
                vws = _VirtualWs(cid, relay_ws, room)
                task = asyncio.ensure_future(_run_client(server, vws, virtuals, cid))
                virtuals[cid] = (vws, task)
                pair = virtuals[cid]
            await pair[0].deliver(data)
    finally:
        pending = []
        for vws, task in virtuals.values():
            vws.mark_closed()
            task.cancel()
            pending.append(task)
        # 等被取消的 handle_client 真正收尾（其 finally 会杀掉 CliTask 子进程），
        # 避免中继断线重连后留下没人管的 claude/codex 进程
        if pending:
            await asyncio.gather(*pending, return_exceptions=True)


async def _run_client(server, vws, virtuals, cid):
    try:
        await server.handle_client(vws)
    finally:
        virtuals.pop(cid, None)
