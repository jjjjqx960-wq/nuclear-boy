# -*- coding: utf-8 -*-
"""WebSocket JSON 消息协议。

客户端 → 服务端:
  {"type":"auth","token":"..."}
  {"type":"run","id":"<uuid>","cli":"claude|codex","prompt":"...","cwd":"...","timeoutSec":600,
   "sessionId":"<可选,续传>","worktree":false}  # worktree=true 时在 cwd 仓库旁建隔离 worktree 执行
  {"type":"cancel","id":"..."}
  {"type":"get_result","id":"..."}   # 断线重连后取回结果或重新挂上输出流
  {"type":"list_tasks"}              # 查询正在执行的任务
  {"type":"ping"}

服务端 → 客户端:
  {"type":"auth_ok","host":"...","clis":{"claude":"2.1.175","codex":"0.139.0"}}
  {"type":"auth_fail","message":"..."}
  {"type":"accepted","id":"..."}
  {"type":"output","id":"...","kind":"text|tool|raw|status","text":"..."}
  {"type":"done","id":"...","exitCode":0,"result":"...","durationMs":1234,"sessionId":"<本次会话ID,可续传>"}
  {"type":"error","id":"...","message":"..."}
  {"type":"pong"}
"""
import json


def encode(msg: dict) -> str:
    return json.dumps(msg, ensure_ascii=False)


def decode(raw) -> dict:
    """解析入站消息；非法 JSON 或非对象时抛 ValueError。"""
    try:
        msg = json.loads(raw)
    except (json.JSONDecodeError, TypeError) as exc:
        raise ValueError(f"非法 JSON: {exc}") from exc
    if not isinstance(msg, dict) or "type" not in msg:
        raise ValueError("消息必须是含 type 字段的 JSON 对象")
    return msg


def auth_ok(host: str, clis: dict) -> str:
    return encode({"type": "auth_ok", "host": host, "clis": clis})


def auth_fail(message: str) -> str:
    return encode({"type": "auth_fail", "message": message})


def accepted(task_id: str) -> str:
    return encode({"type": "accepted", "id": task_id})


def output(task_id: str, kind: str, text: str, seq: int = None) -> str:
    msg = {"type": "output", "id": task_id, "kind": kind, "text": text}
    if seq is not None:
        msg["seq"] = seq
    return encode(msg)


def done(task_id: str, exit_code: int, result: str, duration_ms: int,
         session_id: str = "", worktree_path: str = "", worktree_branch: str = "") -> str:
    msg = {"type": "done", "id": task_id, "exitCode": exit_code,
           "result": result, "durationMs": duration_ms}
    if session_id:
        msg["sessionId"] = session_id
    if worktree_path:
        msg["worktreePath"] = worktree_path
        msg["worktreeBranch"] = worktree_branch
    return encode(msg)


def error(task_id: str, message: str) -> str:
    return encode({"type": "error", "id": task_id, "message": message})


def pong() -> str:
    return encode({"type": "pong"})


def tasks(running: list) -> str:
    """正在执行的任务列表。running 元素: {id, cli, promptPreview, cwd, elapsedMs}"""
    return encode({"type": "tasks", "tasks": running})


def cancelled(task_id: str) -> str:
    return encode({"type": "cancelled", "id": task_id})


# ─── 远程终端（ConPTY）──────────────────────────────────────────────────────
# 客户端 → 服务端:
#   {"type":"term_open","id":"<uuid>","cols":80,"rows":24,"cwd":"...","cmd":"..."}
#   {"type":"term_input","id":"...","data":"<键入>"}
#   {"type":"term_resize","id":"...","cols":100,"rows":30}
#   {"type":"term_close","id":"..."}
# 服务端 → 客户端:
#   {"type":"term_output","id":"...","data":"<含 ANSI 的原始输出>"}
#   {"type":"term_exit","id":"...","code":0}

def term_output(term_id: str, data: str) -> str:
    return encode({"type": "term_output", "id": term_id, "data": data})


def term_exit(term_id: str, code: int) -> str:
    return encode({"type": "term_exit", "id": term_id, "code": code})


# ─── 只读文件浏览 ──────────────────────────────────────────────────────────
# 客户端 → 服务端:
#   {"type":"list_dir","id":"...","path":"D:/proj"}
#   {"type":"read_file","id":"...","path":"D:/proj/a.kt","maxBytes":65536}
# 服务端 → 客户端:
#   {"type":"dir_listing","id":"...","path":"...","entries":[{name,isDir,size}],"truncated":false}
#   {"type":"file_content","id":"...","path":"...","content":"...","size":1234,"truncated":false}

def dir_listing(req_id: str, listing: dict) -> str:
    return encode({"type": "dir_listing", "id": req_id, "path": listing["path"],
                   "entries": listing["entries"], "truncated": listing["truncated"]})


def file_content(req_id: str, result: dict) -> str:
    return encode({"type": "file_content", "id": req_id, "path": result["path"],
                   "content": result["content"], "size": result["size"],
                   "truncated": result["truncated"]})


def file_written(req_id: str, result: dict) -> str:
    return encode({"type": "file_written", "id": req_id,
                   "path": result["path"], "bytes": result["bytes"]})


def sessions_list(req_id: str, sessions: list) -> str:
    return encode({"type": "sessions_list", "id": req_id, "sessions": sessions})
