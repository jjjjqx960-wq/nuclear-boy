# -*- coding: utf-8 -*-
"""权限转发端到端测试：approval=ask 跑 claude，模拟手机批准/拒绝命令执行。

用法: python tests/test_permission_relay.py [--url ws://127.0.0.1:7860]
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


async def run_case(url, token, approve: bool):
    task_id = uuid.uuid4().hex
    label = "批准" if approve else "拒绝"
    async with websockets.connect(url) as ws:
        await ws.send(json.dumps({"type": "auth", "token": token}))
        assert json.loads(await ws.recv())["type"] == "auth_ok"
        import os as _os
        marker = _os.path.join(r"D:\Temp", f"nb-approval-{task_id[:8]}.txt")
        if _os.path.exists(marker):
            _os.remove(marker)
        await ws.send(json.dumps({
            "type": "run", "id": task_id, "cli": "claude",
            "prompt": f"创建文件 {marker}，内容写一行 ok。完成后只回复 done；如果被拒绝无法创建，只回复 被拒绝了。",
            "approval": "ask",
            "timeoutSec": 300,
            "cwd": r"D:\Temp",
        }))
        got_permission_request = False
        while True:
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=300))
            mtype = msg["type"]
            if mtype == "permission_request" and msg["id"] == task_id:
                got_permission_request = True
                print(f"  [{label}] 收到权限请求 tool={msg.get('toolName')} input={json.dumps(msg.get('input'))[:80]}")
                await ws.send(json.dumps({
                    "type": "permission_response", "id": task_id,
                    "approved": approve,
                    "message": "" if approve else "测试拒绝",
                }))
            elif mtype == "output":
                print(f"  [{label}] [{msg['kind']}] {msg['text'][:80]}")
            elif mtype == "done":
                import os as _os
                created = _os.path.exists(marker)
                print(f"  [{label}] done exit={msg['exitCode']} result={msg['result'][:60]} fileCreated={created}")
                assert got_permission_request, "没收到权限请求——审批链路没生效"
                if approve:
                    assert created, "批准了但文件没创建"
                    print(f"[OK] {label}路径：操作被放行并执行，文件已创建")
                    _os.remove(marker)
                else:
                    assert not created, "拒绝了但文件竟被创建"
                    print(f"[OK] {label}路径：操作被拒绝，文件未创建")
                return
            elif mtype == "error":
                print(f"[FAIL] {label}: {msg['message']}")
                sys.exit(1)


async def main_async(url, token):
    await run_case(url, token, approve=True)
    await run_case(url, token, approve=False)
    print("[PASS] 权限转发批准/拒绝双路径全部通过")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="ws://127.0.0.1:7860")
    args = parser.parse_args()
    asyncio.run(main_async(args.url, load_config()["token"]))


if __name__ == "__main__":
    main()
