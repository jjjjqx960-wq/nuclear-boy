# -*- coding: utf-8 -*-
"""断线恢复测试：下发任务后立刻断线，重连用 get_result 取回结果。

用法: python tests/test_disconnect_resume.py [--url ws://127.0.0.1:7860]
"""
import argparse
import asyncio
import json
import sys
import uuid
from pathlib import Path

import websockets

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from core.config import load_config  # noqa: E402


async def auth(ws, token):
    await ws.send(json.dumps({"type": "auth", "token": token}))
    reply = json.loads(await ws.recv())
    assert reply["type"] == "auth_ok", reply
    return reply


async def run_test(url, token):
    task_id = uuid.uuid4().hex

    # 第一段连接：下发任务，收到 accepted 后立刻断线
    async with websockets.connect(url) as ws:
        await auth(ws, token)
        await ws.send(json.dumps({
            "type": "run", "id": task_id, "cli": "claude",
            "prompt": "只回答数字：6乘6等于几",
        }))
        msg = json.loads(await ws.recv())
        assert msg["type"] == "accepted", msg
        print(f"[OK] 任务已接受 id={task_id}，模拟断线")
    # 连接关闭，电脑端任务应继续后台执行

    await asyncio.sleep(4)

    # 第二段连接：重连取回
    async with websockets.connect(url) as ws:
        await auth(ws, token)
        print("[OK] 重连成功，发送 get_result")
        await ws.send(json.dumps({"type": "get_result", "id": task_id}))
        while True:
            msg = json.loads(await ws.recv())
            mtype = msg["type"]
            if mtype == "output":
                print(f"  [{msg['kind']}] {msg['text'][:100]}")
                if "重新接上" in msg.get("text", ""):
                    continue  # 流重挂成功，继续等 done
            elif mtype == "done":
                print(f"[OK] 断线后取回结果 exit={msg['exitCode']} result={msg['result'][:80]} session={msg.get('sessionId','')[:8]}")
                assert "36" in msg["result"], msg["result"]
                print("[PASS] 断线恢复测试通过")
                return
            elif mtype == "error":
                print(f"[FAIL] {msg['message']}")
                sys.exit(1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="ws://127.0.0.1:7860")
    args = parser.parse_args()
    asyncio.run(run_test(args.url, load_config()["token"]))


if __name__ == "__main__":
    main()
