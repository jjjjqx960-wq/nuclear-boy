# -*- coding: utf-8 -*-
"""WebSocket 端到端测试客户端：鉴权 → ping → 下发任务 → 收流式输出。

用法: python tests/test_ws_client.py --url ws://127.0.0.1:7860 --cli claude --prompt "..."
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


async def run_test(url: str, token: str, cli: str, prompt: str, bad_token: bool):
    async with websockets.connect(url) as ws:
        await ws.send(json.dumps({"type": "auth", "token": "wrong" if bad_token else token}))
        reply = json.loads(await ws.recv())
        print("auth =>", reply)
        if bad_token:
            assert reply["type"] == "auth_fail", "错误 token 应被拒绝"
            print("[OK] 错误 token 被正确拒绝")
            return
        assert reply["type"] == "auth_ok", f"鉴权失败: {reply}"

        await ws.send(json.dumps({"type": "ping"}))
        assert json.loads(await ws.recv())["type"] == "pong"
        print("[OK] ping/pong")

        task_id = uuid.uuid4().hex
        await ws.send(json.dumps({
            "type": "run", "id": task_id, "cli": cli, "prompt": prompt,
        }))
        while True:
            msg = json.loads(await ws.recv())
            mtype = msg["type"]
            if mtype == "accepted":
                print("[OK] 任务被接受", msg["id"])
            elif mtype == "output":
                print(f"  [{msg['kind']}] {msg['text'][:120]}")
            elif mtype == "done":
                print(f"[OK] 任务完成 exit={msg['exitCode']} {msg['durationMs']}ms result={msg['result'][:120]}")
                break
            elif mtype == "error":
                print(f"[FAIL] {msg['message']}")
                break


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="ws://127.0.0.1:7860")
    parser.add_argument("--cli", default="claude")
    parser.add_argument("--prompt", default="只回答数字：2+2等于几")
    parser.add_argument("--bad-token", action="store_true", help="测试错误 token 被拒")
    args = parser.parse_args()
    token = load_config()["token"]
    asyncio.run(run_test(args.url, token, args.cli, args.prompt, args.bad_token))


if __name__ == "__main__":
    main()
