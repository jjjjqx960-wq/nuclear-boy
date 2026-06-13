# -*- coding: utf-8 -*-
"""Claude Code 权限审批 MCP 服务（stdio）。

由 claude 进程经 --mcp-config 拉起，claude 的 --permission-prompt-tool 指向
mcp__nbapproval__approve。收到权限请求后经本地 bridge 转发到手机审批，
按用户决定返回 allow/deny。手机 APPROVAL_TIMEOUT_SEC 内未响应则拒绝。

协议: newline-delimited JSON-RPC 2.0（MCP stdio）。
"""
import asyncio
import json
import os
import sys
from pathlib import Path

APPROVAL_TIMEOUT_SEC = 120
PROTOCOL_VERSION = "2024-11-05"

APPROVE_TOOL = {
    "name": "approve",
    "description": "向手机端用户转发权限请求并等待批准",
    "inputSchema": {
        "type": "object",
        "properties": {
            "tool_name": {"type": "string"},
            "input": {"type": "object", "additionalProperties": True},
            "tool_use_id": {"type": "string"},
        },
        "additionalProperties": True,
    },
}


async def ask_phone(tool_name: str, tool_input: dict):
    """经 bridge 把权限请求转发给手机，返回 (approved, message)。"""
    import websockets  # 延迟导入，保证 tools/list 等握手不依赖网络库

    task_id = os.environ.get("NB_TASK_ID", "")
    if not task_id:
        return False, "缺少 NB_TASK_ID，无法定位任务"
    with open(Path(__file__).parent / "config.json", encoding="utf-8") as f:
        cfg = json.load(f)
    uri = f"ws://127.0.0.1:{cfg['port']}"
    async with websockets.connect(uri) as ws:
        await ws.send(json.dumps({"type": "auth", "token": cfg["token"]}))
        reply = json.loads(await ws.recv())
        if reply.get("type") != "auth_ok":
            return False, "审批服务连不上 bridge"
        await ws.send(json.dumps({
            "type": "permission_request", "id": task_id,
            "toolName": tool_name, "input": tool_input,
        }))
        # 绝对截止时间：无关消息（ping 等）不应一次次刷新超时窗口
        deadline = asyncio.get_event_loop().time() + APPROVAL_TIMEOUT_SEC
        while True:
            remaining = deadline - asyncio.get_event_loop().time()
            if remaining <= 0:
                return False, "审批超时，自动拒绝"
            msg = json.loads(await asyncio.wait_for(ws.recv(), timeout=remaining))
            if msg.get("type") == "permission_response" and msg.get("id") == task_id:
                return bool(msg.get("approved")), msg.get("message", "")


def permission_result(approved: bool, tool_input: dict, message: str) -> str:
    """组装 claude 要求的权限回复 JSON。"""
    if approved:
        return json.dumps({"behavior": "allow", "updatedInput": tool_input})
    return json.dumps({"behavior": "deny", "message": message or "用户在手机上拒绝了该操作"})


async def handle_request(req: dict):
    method = req.get("method", "")
    rid = req.get("id")
    if method == "initialize":
        return {"jsonrpc": "2.0", "id": rid, "result": {
            "protocolVersion": req.get("params", {}).get("protocolVersion", PROTOCOL_VERSION),
            "capabilities": {"tools": {}},
            "serverInfo": {"name": "nbapproval", "version": "1.0.0"},
        }}
    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": rid, "result": {"tools": [APPROVE_TOOL]}}
    if method == "tools/call":
        params = req.get("params", {})
        args = params.get("arguments", {})
        tool_name = args.get("tool_name", "?")
        tool_input = args.get("input") or {}
        try:
            with open(Path(__file__).parent / "mcp_approval.log", "a", encoding="utf-8") as lf:
                lf.write(f"tools/call tool={tool_name} task={os.environ.get('NB_TASK_ID','')}\n")
        except Exception:
            pass
        try:
            approved, message = await ask_phone(tool_name, tool_input)
        except asyncio.TimeoutError:
            approved, message = False, f"手机 {APPROVAL_TIMEOUT_SEC}s 内未审批，自动拒绝"
        except Exception as exc:  # 审批通道故障必须拒绝而非放行
            approved, message = False, f"审批通道异常: {exc}"
        return {"jsonrpc": "2.0", "id": rid, "result": {
            "content": [{"type": "text", "text": permission_result(approved, tool_input, message)}],
        }}
    if rid is not None:
        return {"jsonrpc": "2.0", "id": rid,
                "error": {"code": -32601, "message": f"method not found: {method}"}}
    return None  # notification，无需回复


def main():
    # Windows 上 asyncio.connect_read_pipe 不支持 stdin 管道，用同步逐行读，
    # 每个请求单独跑一次事件循环处理（ask_phone 是 async）。
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except json.JSONDecodeError:
            continue
        resp = asyncio.run(handle_request(req))
        if resp is not None:
            sys.stdout.write(json.dumps(resp, ensure_ascii=False) + "\n")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
