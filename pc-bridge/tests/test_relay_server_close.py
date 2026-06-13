# -*- coding: utf-8 -*-
"""验证中继把电脑端主动关闭（server_close，如鉴权封禁 4029）透传给手机。

用法: python tests/test_relay_server_close.py
"""
import asyncio
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import websockets
from relay.relay_server import serve_forever

HOST = "127.0.0.1"
PORT = 8973
KEY = "tkey"


async def run():
    server_task = asyncio.ensure_future(serve_forever(HOST, PORT, KEY))
    await asyncio.sleep(0.4)
    base = f"ws://{HOST}:{PORT}"
    try:
        async with websockets.connect(f"{base}/agent/room1?key={KEY}") as agent:
            async with websockets.connect(f"{base}/client/room1?key={KEY}") as client:
                # 手机先发一条，让中继分配 cid 并转给电脑
                await client.send("hello")
                env = json.loads(await agent.recv())
                cid = env["_cid"]
                assert env["_data"] == "hello", env

                # 电脑带码主动关闭这台手机
                await agent.send(json.dumps({
                    "_cid": cid, "_event": "server_close",
                    "_code": 4029, "_reason": "too many auth failures",
                }))
                # 手机这一端应收到带 4029 的关闭
                try:
                    await asyncio.wait_for(client.recv(), timeout=3)
                    assert False, "应已被关闭"
                except websockets.ConnectionClosed as exc:
                    assert exc.code == 4029, f"期望 4029 实际 {exc.code}"
                    print(f"[PASS] 中继透传 server_close：手机收到关闭码 {exc.code}")
    finally:
        server_task.cancel()


if __name__ == "__main__":
    asyncio.run(run())
