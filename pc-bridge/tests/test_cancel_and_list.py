# -*- coding: utf-8 -*-
"""取消与任务列表测试：下发任务 → list_tasks 看到它 → cancel → 收到取消确认和错误收尾。

用法: python tests/test_cancel_and_list.py [--url ws://127.0.0.1:7860]
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


async def run_test(url, token):
    task_id = uuid.uuid4().hex
    async with websockets.connect(url) as ws:
        await ws.send(json.dumps({"type": "auth", "token": token}))
        assert json.loads(await ws.recv())["type"] == "auth_ok"

        # 下发一个长任务
        await ws.send(json.dumps({
            "type": "run", "id": task_id, "cli": "claude",
            "prompt": "请详细写一篇 2000 字的快速排序原理讲解",
        }))
        msg = json.loads(await ws.recv())
        assert msg["type"] == "accepted", msg
        print(f"[OK] 长任务已接受 id={task_id[:8]}")
        await asyncio.sleep(3)

        # 任务列表应包含它
        await ws.send(json.dumps({"type": "list_tasks"}))
        while True:
            msg = json.loads(await ws.recv())
            if msg["type"] == "tasks":
                break
        ids = [t["id"] for t in msg["tasks"]]
        assert task_id in ids, msg
        entry = next(t for t in msg["tasks"] if t["id"] == task_id)
        print(f"[OK] list_tasks 命中: cli={entry['cli']} elapsed={entry['elapsedMs']}ms preview={entry['promptPreview'][:30]}")

        # 取消
        await ws.send(json.dumps({"type": "cancel", "id": task_id}))
        got_cancelled, got_error = False, False
        while not (got_cancelled and got_error):
            msg = json.loads(await ws.recv())
            if msg["type"] == "cancelled" and msg["id"] == task_id:
                got_cancelled = True
                print("[OK] 收到取消确认")
            elif msg["type"] == "error" and msg["id"] == task_id:
                got_error = True
                print(f"[OK] 任务以取消错误收尾: {msg['message']}")
            elif msg["type"] == "done":
                print("[FAIL] 任务居然跑完了，取消未生效")
                sys.exit(1)

        # 取消后列表应为空
        await ws.send(json.dumps({"type": "list_tasks"}))
        while True:
            msg = json.loads(await ws.recv())
            if msg["type"] == "tasks":
                break
        assert task_id not in [t["id"] for t in msg["tasks"]], msg
        print("[OK] 取消后任务已从列表移除")

        # 取消不存在的任务应报错
        await ws.send(json.dumps({"type": "cancel", "id": "nonexistent"}))
        msg = json.loads(await ws.recv())
        assert msg["type"] == "error", msg
        print("[PASS] 取消与任务列表测试全部通过")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="ws://127.0.0.1:7860")
    args = parser.parse_args()
    asyncio.run(run_test(args.url, load_config()["token"]))


if __name__ == "__main__":
    main()
