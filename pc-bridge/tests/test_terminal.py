# -*- coding: utf-8 -*-
"""远程终端（ConPTY）端到端测试：经 WebSocket 开终端、键入、收输出、关闭。

用法: python tests/test_terminal.py
"""
import asyncio
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import websockets
from core.server import BridgeServer

HOST = "127.0.0.1"
PORT = 8975
TOKEN = "termtoken"


def _cfg():
    return {
        "host": HOST, "port": PORT, "token": TOKEN, "default_cwd": "D:\\",
        "max_concurrent_tasks": 2, "task_timeout_sec": 60,
        "clis": {"claude": {"command": "claude", "args": [], "prompt_via": "stdin"}},
    }


async def run():
    server = BridgeServer(_cfg())
    srv = await websockets.serve(server.handle_client, HOST, PORT, max_size=2 ** 22)
    try:
        async with websockets.connect(f"ws://{HOST}:{PORT}", max_size=2 ** 22) as ws:
            await ws.send(json.dumps({"type": "auth", "token": TOKEN}))
            auth = json.loads(await ws.recv())
            assert auth["type"] == "auth_ok", auth

            # 开终端
            await ws.send(json.dumps({"type": "term_open", "id": "t1", "cols": 80, "rows": 24, "cmd": "cmd.exe"}))
            accepted = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
            assert accepted["type"] == "accepted" and accepted["id"] == "t1", accepted

            # 等初始 banner 输出
            got_output = False
            for _ in range(20):
                msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
                if msg["type"] == "term_output" and msg["id"] == "t1":
                    got_output = True
                    break
            assert got_output, "没收到终端初始输出"

            # 键入命令，期望回显里出现标记
            await ws.send(json.dumps({"type": "term_input", "id": "t1", "data": "echo NB_TERM_OK\r\n"}))
            buf = ""
            found = False
            for _ in range(40):
                msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=5))
                if msg["type"] == "term_output" and msg["id"] == "t1":
                    buf += msg["data"]
                    if "NB_TERM_OK" in buf:
                        found = True
                        break
            assert found, f"命令输出未出现，收到: {buf[-200:]!r}"
            print("[PASS] 终端开启+键入+输出回传通过")

            # resize 不报错
            await ws.send(json.dumps({"type": "term_resize", "id": "t1", "cols": 120, "rows": 40}))

            # 关闭终端
            await ws.send(json.dumps({"type": "term_close", "id": "t1"}))
            await asyncio.sleep(0.5)
            assert server.terminals.get("t1") is None, "终端未被清理"
            print("[PASS] 终端 resize + 关闭清理通过")
    finally:
        for tid in list(server.terminals):
            await server.close_terminal(tid)
        srv.close()
        await srv.wait_closed()


if __name__ == "__main__":
    asyncio.run(run())
    print("[PASS] 远程终端全链路通过")
