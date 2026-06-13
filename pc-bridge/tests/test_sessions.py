# -*- coding: utf-8 -*-
"""会话列表：list_sessions 单元 + 经 WebSocket 端到端。

用法: python tests/test_sessions.py
"""
import asyncio
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import websockets
from core import sessions
from core.server import BridgeServer

HOST = "127.0.0.1"
PORT = 8981
TOKEN = "sess-token"


def test_unit():
    s = sessions.list_sessions(limit=3)
    assert isinstance(s, list)
    for x in s:
        assert "sessionId" in x and "cwd" in x and "preview" in x and "mtimeMs" in x, x
        assert x["cli"] == "claude"
    print(f"[PASS] list_sessions 单元：取到 {len(s)} 个会话")


async def test_e2e():
    cfg = {"host": HOST, "port": PORT, "token": TOKEN, "default_cwd": ".",
           "max_concurrent_tasks": 2, "task_timeout_sec": 60,
           "clis": {"claude": {"command": "claude", "args": [], "prompt_via": "stdin"}}}
    server = BridgeServer(cfg)
    srv = await websockets.serve(server.handle_client, HOST, PORT, max_size=2 ** 22)
    try:
        async with websockets.connect(f"ws://{HOST}:{PORT}") as ws:
            await ws.send(json.dumps({"type": "auth", "token": TOKEN}))
            assert json.loads(await ws.recv())["type"] == "auth_ok"
            await ws.send(json.dumps({"type": "list_sessions", "id": "s1", "limit": 5}))
            reply = json.loads(await ws.recv())
            assert reply["type"] == "sessions_list" and isinstance(reply["sessions"], list), reply
            print(f"[PASS] 会话列表端到端：返回 {len(reply['sessions'])} 个")
    finally:
        srv.close()
        await srv.wait_closed()


if __name__ == "__main__":
    test_unit()
    asyncio.run(test_e2e())
    print("[PASS] 会话列表全部通过")
