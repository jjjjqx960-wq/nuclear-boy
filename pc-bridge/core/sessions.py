# -*- coding: utf-8 -*-
"""列出电脑上 Claude Code 的历史会话（借鉴 claudecodeui sessionManager）。

Claude Code 把每个会话存成 ~/.claude/projects/<编码cwd>/<sessionId>.jsonl，
文件名即 sessionId，内容含 cwd 与首条用户消息。手机据此浏览历史会话、挑一个
传给 pc_cli_run 的 session 参数续聊，不用记 UUID。只读，不动这些文件。
"""
import glob
import json
import os

PROJECTS_DIR = os.path.join(os.path.expanduser("~"), ".claude", "projects")
PREVIEW_MAX = 80
SCAN_DEFAULT = 20
SCAN_HARD_MAX = 100


def _first_user_and_cwd(path: str):
    """只读到拿齐首条用户消息和 cwd 就停，避免整文件解析。"""
    cwd = ""
    preview = ""
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            for line in f:
                try:
                    o = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if not cwd and o.get("cwd"):
                    cwd = o["cwd"]
                if not preview and o.get("type") == "user":
                    msg = o.get("message", {})
                    content = msg.get("content") if isinstance(msg, dict) else None
                    if isinstance(content, str):
                        preview = content
                    elif isinstance(content, list):
                        for part in content:
                            if isinstance(part, dict) and part.get("type") == "text":
                                preview = part.get("text", "")
                                break
                if cwd and preview:
                    break
    except OSError:
        pass
    return preview[:PREVIEW_MAX], cwd


def list_sessions(limit: int = SCAN_DEFAULT, cwd_filter: str = "") -> list:
    """返回最近的 Claude 会话列表，按修改时间倒序。

    每项: {sessionId, cli, cwd, preview, mtimeMs}。cwd_filter 非空时只留该目录的会话。
    """
    limit = max(1, min(int(limit or SCAN_DEFAULT), SCAN_HARD_MAX))
    files = glob.glob(os.path.join(PROJECTS_DIR, "*", "*.jsonl"))
    files.sort(key=lambda p: os.path.getmtime(p), reverse=True)
    out = []
    norm_filter = os.path.normcase(os.path.abspath(cwd_filter)) if cwd_filter else ""
    for path in files:
        if len(out) >= limit:
            break
        preview, cwd = _first_user_and_cwd(path)
        if norm_filter and (not cwd or os.path.normcase(os.path.abspath(cwd)) != norm_filter):
            continue
        out.append({
            "sessionId": os.path.splitext(os.path.basename(path))[0],
            "cli": "claude",
            "cwd": cwd,
            "preview": preview,
            "mtimeMs": int(os.path.getmtime(path) * 1000),
        })
    return out
