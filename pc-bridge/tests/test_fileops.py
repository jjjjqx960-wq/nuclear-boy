# -*- coding: utf-8 -*-
"""只读文件浏览：fileops 单元 + 经 WebSocket 的 list_dir/read_file 端到端。

用法: python tests/test_fileops.py
"""
import asyncio
import json
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import websockets
from core import fileops
from core.server import BridgeServer

HOST = "127.0.0.1"
PORT = 8977
TOKEN = "filetoken"


def test_unit():
    with tempfile.TemporaryDirectory() as d:
        f = os.path.join(d, "hello.txt")
        with open(f, "w", encoding="utf-8") as fh:
            fh.write("你好 nuclear")
        listing = fileops.list_dir(d)
        assert any(e["name"] == "hello.txt" and not e["isDir"] for e in listing["entries"]), listing
        content = fileops.read_file(f)
        assert content["content"] == "你好 nuclear", content
        # 截断
        small = fileops.read_file(f, max_bytes=3)
        assert small["truncated"] is True
        # 写入（覆盖 + 追加）
        w = fileops.write_file(f, "新内容")
        assert w["bytes"] > 0 and fileops.read_file(f)["content"] == "新内容", w
        fileops.write_file(f, "X", append=True)
        assert fileops.read_file(f)["content"] == "新内容X"
        # 父目录不存在应拒绝
        try:
            fileops.write_file(os.path.join(d, "no", "sub", "a.txt"), "x")
            assert False, "应拒绝"
        except ValueError:
            pass
        # 错误路径
        for bad in [lambda: fileops.read_file(os.path.join(d, "nope")),
                    lambda: fileops.list_dir(os.path.join(d, "nope")),
                    lambda: fileops.read_file(d)]:
            try:
                bad(); assert False, "应抛 ValueError"
            except ValueError:
                pass
    print("[PASS] fileops 单元测试通过")


async def test_e2e():
    cfg = {"host": HOST, "port": PORT, "token": TOKEN, "default_cwd": ".",
           "max_concurrent_tasks": 2, "task_timeout_sec": 60,
           "clis": {"claude": {"command": "claude", "args": [], "prompt_via": "stdin"}}}
    server = BridgeServer(cfg)
    srv = await websockets.serve(server.handle_client, HOST, PORT, max_size=2 ** 22)
    with tempfile.TemporaryDirectory() as d:
        fp = os.path.join(d, "a.txt")
        with open(fp, "w", encoding="utf-8") as fh:
            fh.write("nbcontent")
        try:
            async with websockets.connect(f"ws://{HOST}:{PORT}") as ws:
                await ws.send(json.dumps({"type": "auth", "token": TOKEN}))
                assert json.loads(await ws.recv())["type"] == "auth_ok"

                await ws.send(json.dumps({"type": "list_dir", "id": "d1", "path": d}))
                ld = json.loads(await ws.recv())
                assert ld["type"] == "dir_listing" and any(e["name"] == "a.txt" for e in ld["entries"]), ld

                await ws.send(json.dumps({"type": "read_file", "id": "r1", "path": fp}))
                fc = json.loads(await ws.recv())
                assert fc["type"] == "file_content" and fc["content"] == "nbcontent", fc

                await ws.send(json.dumps({"type": "read_file", "id": "r2", "path": os.path.join(d, "missing")}))
                err = json.loads(await ws.recv())
                assert err["type"] == "error", err

                wp = os.path.join(d, "new.txt")
                await ws.send(json.dumps({"type": "write_file", "id": "w1", "path": wp, "content": "写入测试"}))
                fw = json.loads(await ws.recv())
                assert fw["type"] == "file_written" and fw["bytes"] > 0, fw
                with open(wp, encoding="utf-8") as fh:
                    assert fh.read() == "写入测试"
            print("[PASS] 文件浏览端到端通过")
        finally:
            srv.close()
            await srv.wait_closed()


if __name__ == "__main__":
    test_unit()
    asyncio.run(test_e2e())
    print("[PASS] 只读文件浏览全部通过")
