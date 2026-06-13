# -*- coding: utf-8 -*-
"""加密会话端到端：手机用 token 派生密钥加密握手，全程密文，中继看不到明文。

用法: python tests/test_encrypted_session.py
"""
import asyncio
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import websockets
from core import crypto
from core.server import BridgeServer

HOST = "127.0.0.1"
PORT = 8979
TOKEN = "enc-token-xyz"


def _cfg():
    return {"host": HOST, "port": PORT, "token": TOKEN, "default_cwd": ".",
            "max_concurrent_tasks": 2, "task_timeout_sec": 60,
            "clis": {"claude": {"command": "claude", "args": [], "prompt_via": "stdin"}}}


async def run():
    server = BridgeServer(_cfg())
    srv = await websockets.serve(server.handle_client, HOST, PORT, max_size=2 ** 22)
    key = crypto.derive_key(TOKEN)
    try:
        # 加密客户端
        async with websockets.connect(f"ws://{HOST}:{PORT}") as ws:
            await ws.send(crypto.envelope(key, json.dumps({"type": "auth"})))
            raw = await ws.recv()
            # 线上必须是密文信封（中继看到的就是这个）
            env = json.loads(raw)
            assert "enc" in env, "返回未加密！"
            auth = json.loads(crypto.decrypt(key, env["enc"]))
            assert auth["type"] == "auth_ok", auth

            await ws.send(crypto.envelope(key, json.dumps({"type": "ping"})))
            pong = json.loads(crypto.decrypt(key, json.loads(await ws.recv())["enc"]))
            assert pong["type"] == "pong", pong
            print("[PASS] 加密握手 + 加密 ping/pong 通过")

        # 错误 token 的加密客户端应被拒
        async with websockets.connect(f"ws://{HOST}:{PORT}") as ws2:
            badkey = crypto.derive_key("wrong-token")
            await ws2.send(crypto.envelope(badkey, json.dumps({"type": "auth"})))
            try:
                await asyncio.wait_for(ws2.recv(), timeout=3)
                # 可能直接 close
            except Exception:
                pass
            assert ws2.close_code == 4003 or ws2.close_code is not None
            print("[PASS] 错误 token 加密握手被拒")

        # 明文客户端仍可用（向后兼容）
        async with websockets.connect(f"ws://{HOST}:{PORT}") as ws3:
            await ws3.send(json.dumps({"type": "auth", "token": TOKEN}))
            auth = json.loads(await ws3.recv())
            assert auth["type"] == "auth_ok", auth
            print("[PASS] 明文客户端向后兼容")
    finally:
        srv.close()
        await srv.wait_closed()


if __name__ == "__main__":
    asyncio.run(run())
    print("[PASS] 加密会话全链路通过")
