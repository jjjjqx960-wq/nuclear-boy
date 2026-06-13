# -*- coding: utf-8 -*-
"""历史增量同步：断线重连用 sinceSeq 只补发漏掉的输出，不重复不丢中间过程。

用法: python tests/test_incremental_sync.py
"""
import asyncio
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core import protocol
from core.server import BridgeServer


class FakeWs:
    def __init__(self):
        self.sent = []

    async def send(self, payload):
        self.sent.append(json.loads(payload))


def _cfg():
    return {"host": "127.0.0.1", "port": 0, "token": "t", "default_cwd": ".",
            "max_concurrent_tasks": 2, "task_timeout_sec": 60,
            "clis": {"claude": {"command": "claude", "args": [], "prompt_via": "stdin"}}}


async def run():
    server = BridgeServer(_cfg())
    tid = "task1"
    # 模拟任务进行中，已产生 5 条带 seq 的输出缓存
    server.tasks[tid] = object()
    server.task_outputs[tid] = [
        (i, protocol.output(tid, "text", f"line{i}", seq=i)) for i in range(5)
    ]

    # 手机已收到 seq 0,1,2（maxSeq=2），断线后用 sinceSeq=3 重连
    ws = FakeWs()
    await server.handle_get_result(ws, tid, since_seq=3)
    outputs = [m for m in ws.sent if m["type"] == "output" and m.get("kind") == "text"]
    seqs = [m["seq"] for m in outputs]
    assert seqs == [3, 4], f"应只补发 3,4，实际 {seqs}"
    # 末尾应有重挂状态消息
    assert any(m["type"] == "output" and m.get("kind") == "status" for m in ws.sent), ws.sent
    print(f"[PASS] 增量补发：sinceSeq=3 只补 {seqs}，不重复不丢")

    # sinceSeq=0 应补全部 5 条
    ws2 = FakeWs()
    await server.handle_get_result(ws2, tid, since_seq=0)
    seqs2 = [m["seq"] for m in ws2.sent if m["type"] == "output" and m.get("kind") == "text"]
    assert seqs2 == [0, 1, 2, 3, 4], seqs2
    print(f"[PASS] sinceSeq=0 补全 {seqs2}")

    # sinceSeq 超过最大：不补任何输出
    ws3 = FakeWs()
    await server.handle_get_result(ws3, tid, since_seq=99)
    seqs3 = [m["seq"] for m in ws3.sent if m["type"] == "output" and m.get("kind") == "text"]
    assert seqs3 == [], seqs3
    print("[PASS] sinceSeq 超界不补发")

    # 已完成任务：先补漏掉输出再发 done
    server.tasks.pop(tid, None)
    server._store_result(tid, protocol.done(tid, 0, "答案", 100))
    ws4 = FakeWs()
    await server.handle_get_result(ws4, tid, since_seq=4)
    types = [m["type"] for m in ws4.sent]
    assert "done" in types and types[-1] == "done", types
    print("[PASS] 完成任务补发后发 done")


if __name__ == "__main__":
    asyncio.run(run())
    print("[PASS] 历史增量同步全部通过")
