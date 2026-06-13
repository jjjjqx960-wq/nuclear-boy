# -*- coding: utf-8 -*-
"""worktree 隔离测试：建临时 git 仓库 → worktree=true 跑任务 → 验证改动落在 worktree、主仓库干净。

用法: python tests/test_worktree.py [--url ws://127.0.0.1:7860]
"""
import argparse
import asyncio
import json
import os
import shutil
import subprocess
import sys
import uuid
from pathlib import Path

import websockets

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from core.config import load_config  # noqa: E402

TEST_REPO = r"D:\Temp\nb-wt-test\repo"


def setup_repo():
    root = os.path.dirname(TEST_REPO)
    if os.path.exists(root):
        shutil.rmtree(root, ignore_errors=True)
    os.makedirs(TEST_REPO)
    run = lambda *a: subprocess.run(a, cwd=TEST_REPO, capture_output=True, check=True)
    run("git", "init")
    run("git", "config", "user.email", "test@test.local")
    run("git", "config", "user.name", "test")
    with open(os.path.join(TEST_REPO, "README.md"), "w", encoding="utf-8") as f:
        f.write("test repo\n")
    run("git", "add", ".")
    run("git", "commit", "-m", "init")


async def run_test(url, token):
    setup_repo()
    task_id = uuid.uuid4().hex
    async with websockets.connect(url) as ws:
        await ws.send(json.dumps({"type": "auth", "token": token}))
        assert json.loads(await ws.recv())["type"] == "auth_ok"

        await ws.send(json.dumps({
            "type": "run", "id": task_id, "cli": "claude",
            "prompt": "在当前目录创建文件 hello.txt，内容只有一行：hi。完成后只回复：done",
            "cwd": TEST_REPO,
            "worktree": True,
        }))
        worktree_path = ""
        while True:
            msg = json.loads(await ws.recv())
            if msg["type"] == "output":
                print(f"  [{msg['kind']}] {msg['text'][:90]}")
            elif msg["type"] == "done":
                worktree_path = msg.get("worktreePath", "")
                print(f"[OK] 任务完成 exit={msg['exitCode']} worktree={worktree_path} branch={msg.get('worktreeBranch','')}")
                break
            elif msg["type"] == "error":
                print(f"[FAIL] {msg['message']}")
                sys.exit(1)

    assert worktree_path, "done 消息缺少 worktreePath"
    assert os.path.isfile(os.path.join(worktree_path, "hello.txt")), "worktree 里没有 hello.txt"
    assert not os.path.exists(os.path.join(TEST_REPO, "hello.txt")), "主仓库被污染了！"
    status = subprocess.run(["git", "-C", TEST_REPO, "status", "--porcelain"],
                            capture_output=True, text=True)
    assert status.stdout.strip() == "", f"主仓库工作区不干净: {status.stdout}"
    print("[OK] 改动只落在 worktree，主仓库干净")

    # 清理命令验证
    clean = subprocess.run(
        [sys.executable, str(Path(__file__).resolve().parent.parent / "bridge.py"),
         "clean-worktrees", "--repo", TEST_REPO],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    print(clean.stdout.strip())
    assert "清理了 1 个" in clean.stdout, clean.stdout + clean.stderr
    assert not os.path.exists(worktree_path), "worktree 目录未被清理"
    print("[PASS] worktree 隔离测试全部通过")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="ws://127.0.0.1:7860")
    args = parser.parse_args()
    asyncio.run(run_test(args.url, load_config()["token"]))


if __name__ == "__main__":
    main()
