# -*- coding: utf-8 -*-
"""CLI 任务执行器：以子进程方式运行 claude/codex 并流式提取输出。"""
import asyncio
import json
import os
import shutil
import subprocess
import time

OUTPUT_LINE_LIMIT = 8000        # 单行输出截断长度
DEFAULT_TIMEOUT_SEC = 900


async def prepare_worktree(repo_dir: str, task_id: str):
    """为任务在仓库旁创建独立 git worktree，返回 (worktree 路径, 分支名)。

    目录布局: <repo 上级>/.nb-worktrees/<repo 名>-<task 前 8 位>，分支 nb/<task 前 8 位>。
    repo_dir 不是 git 仓库时抛 RuntimeError。
    """
    probe = await asyncio.create_subprocess_exec(
        "git", "-C", repo_dir, "rev-parse", "--is-inside-work-tree",
        stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE,
    )
    out, _ = await probe.communicate()
    if probe.returncode != 0 or out.decode().strip() != "true":
        raise RuntimeError(f"工作目录不是 git 仓库，无法创建隔离 worktree: {repo_dir}")

    short = task_id[:8]
    repo_name = os.path.basename(os.path.normpath(repo_dir))
    wt_root = os.path.join(os.path.dirname(os.path.normpath(repo_dir)), ".nb-worktrees")
    os.makedirs(wt_root, exist_ok=True)
    wt_path = os.path.join(wt_root, f"{repo_name}-{short}")
    branch = f"nb/{short}"
    # 同前缀 task_id 极罕见，但若残留半成品 worktree 目录会永久卡住该前缀——先清理
    if os.path.exists(wt_path):
        cleanup = await asyncio.create_subprocess_exec(
            "git", "-C", repo_dir, "worktree", "remove", "--force", wt_path,
            stdout=asyncio.subprocess.DEVNULL, stderr=asyncio.subprocess.DEVNULL,
        )
        await cleanup.wait()

    proc = await asyncio.create_subprocess_exec(
        "git", "-C", repo_dir, "worktree", "add", "-b", branch, wt_path, "HEAD",
        stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE,
    )
    _, err = await proc.communicate()
    if proc.returncode != 0:
        raise RuntimeError(f"创建 worktree 失败: {err.decode('utf-8', errors='replace').strip()[:200]}")
    return wt_path, branch


def resolve_command(command: str):
    """解析 CLI 可执行文件。npm 的 .cmd 包装需经 cmd /c 启动。"""
    path = shutil.which(command)
    if path is None:
        return None
    if path.lower().endswith((".cmd", ".bat")):
        return ["cmd", "/c", path]
    return [path]


def detect_cli_versions(clis_cfg: dict) -> dict:
    """探测各 CLI 版本，探测失败的标记为 missing。"""
    versions = {}
    for name, cfg in clis_cfg.items():
        argv = resolve_command(cfg["command"])
        if argv is None:
            versions[name] = "missing"
            continue
        try:
            out = subprocess.run(
                argv + ["--version"], capture_output=True, text=True,
                timeout=30, encoding="utf-8", errors="replace",
            )
            versions[name] = (out.stdout or out.stderr).strip().splitlines()[0][:80] \
                if (out.stdout or out.stderr).strip() else "unknown"
        except (subprocess.SubprocessError, OSError) as exc:
            versions[name] = f"error: {exc}"
    return versions


def _extract_claude_event(line: str):
    """从 claude stream-json 行提取 (kind, text)；无关行返回 None。

    session 事件单独返回 ("session", session_id)，供任务记录续传 ID。
    """
    try:
        evt = json.loads(line)
    except json.JSONDecodeError:
        return ("raw", line) if line.strip() else None
    etype = evt.get("type")
    if etype == "system" and evt.get("subtype") == "init" and evt.get("session_id"):
        return ("session", evt["session_id"])
    if etype == "assistant":
        parts = []
        for block in (evt.get("message") or {}).get("content") or []:
            if block.get("type") == "text" and block.get("text"):
                parts.append(block["text"])
            elif block.get("type") == "tool_use":
                parts.append(f"[工具] {block.get('name', '?')}")
        return ("text", "\n".join(parts)) if parts else None
    if etype == "result":
        return ("result", evt.get("result") or "")
    return None


def _extract_codex_event(line: str):
    """从 codex exec --json 行提取 (kind, text)；尽量宽松兼容不同版本。"""
    try:
        evt = json.loads(line)
    except json.JSONDecodeError:
        return ("raw", line) if line.strip() else None
    if evt.get("type") == "thread.started" and evt.get("thread_id"):
        return ("session", evt["thread_id"])
    item = evt.get("item") or evt.get("msg") or {}
    itype = item.get("type") or evt.get("type") or ""
    text = item.get("text") or item.get("message") or ""
    if "agent_message" in itype and text:
        return ("text", text)
    if "command" in itype:
        cmd = item.get("command") or text
        return ("tool", f"[命令] {cmd}") if cmd else None
    if "reasoning" in itype:
        return None  # 思考过程不回传
    if text:
        return ("raw", text)
    return None


def _extract_opencode_event(line: str):
    """从 opencode run --format json 行提取 (kind, text)。

    事件形如 {"type":"text","sessionID":"ses_...","part":{"type":"text","text":"..."}}。
    会话 ID 取首个事件的 sessionID。
    """
    try:
        evt = json.loads(line)
    except json.JSONDecodeError:
        return ("raw", line) if line.strip() else None
    etype = evt.get("type") or ""
    part = evt.get("part") or {}
    if etype == "text" and part.get("text"):
        return ("text", part["text"])
    if "tool" in etype:
        tool_name = part.get("tool") or part.get("name") or etype
        return ("tool", f"[工具] {tool_name}")
    if etype == "step_start" and evt.get("sessionID"):
        return ("session", evt["sessionID"])
    return None


EXTRACTORS = {
    "claude": _extract_claude_event,
    "codex": _extract_codex_event,
    "opencode": _extract_opencode_event,
}


async def _kill_tree(proc):
    """Windows 上用 taskkill 杀整棵进程树，避免残留孤儿进程。"""
    if proc.returncode is not None:
        return
    try:
        killer = await asyncio.create_subprocess_exec(
            "taskkill", "/T", "/F", "/PID", str(proc.pid),
            stdout=asyncio.subprocess.DEVNULL, stderr=asyncio.subprocess.DEVNULL,
        )
        await killer.wait()
        rc = killer.returncode
    except Exception:
        rc = -1
    # taskkill 失败（进程刚退出/权限/创建作业对象逃逸 /T）时，兜底直接杀直接子进程
    if (rc is None or rc != 0) and proc.returncode is None:
        try:
            proc.kill()
        except ProcessLookupError:
            pass
        except Exception:
            pass


class CliTask:
    """一次 CLI 任务的生命周期：启动、流式输出、完成/取消/超时。"""

    def __init__(self, task_id: str, cli: str, prompt: str, cwd: str,
                 timeout_sec: int, cli_cfg: dict, resume_session_id: str = "",
                 use_worktree: bool = False, approval_mode: str = "auto"):
        self.approval_mode = approval_mode
        self.task_id = task_id
        self.cli = cli
        self.prompt = prompt
        self.cwd = cwd
        self.timeout_sec = timeout_sec or DEFAULT_TIMEOUT_SEC
        self.cli_cfg = cli_cfg
        self.resume_session_id = resume_session_id
        self.use_worktree = use_worktree
        self.worktree_path = ""
        self.worktree_branch = ""
        self.session_id = ""  # 本次任务的会话 ID（claude init 事件提供）
        self.started_at = time.monotonic()
        self.proc = None
        self.cancelled = False

    def elapsed_ms(self) -> int:
        return int((time.monotonic() - self.started_at) * 1000)

    async def cancel(self):
        self.cancelled = True
        if self.proc is not None:
            await _kill_tree(self.proc)

    async def run(self, on_event):
        """执行任务。on_event(kind, text) 推送增量；返回 (exit_code, result, duration_ms)。"""
        start = time.monotonic()
        argv = resolve_command(self.cli_cfg["command"])
        if argv is None:
            raise RuntimeError(f"电脑上找不到 {self.cli_cfg['command']}，请确认已安装并在 PATH 中")
        base_args = list(self.cli_cfg.get("args") or [])

        if self.approval_mode == "ask":
            if self.cli != "claude":
                raise RuntimeError("审批模式目前仅支持 claude，去掉 approval 参数或换用 claude")
            # 把放行模式换成默认模式 + 手机审批工具
            cleaned = []
            skip_next = False
            for arg in base_args:
                if skip_next:
                    skip_next = False
                    continue
                if arg == "--permission-mode":
                    skip_next = True
                    continue
                cleaned.append(arg)
            bridge_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            mcp_cfg = os.path.join(bridge_root, "nbapproval-mcp.json")
            settings = os.path.join(bridge_root, "approval-settings.json")
            base_args = cleaned + [
                "--permission-mode", "default",
                "--permission-prompt-tool", "mcp__nbapproval__approve",
                "--mcp-config", mcp_cfg,
                "--strict-mcp-config",  # 只加载审批服务，不继承用户级 MCP
                "--settings", settings,  # 隔离本机 permissions.allow，确保审批对所有工具生效
            ]
        argv = argv + base_args

        if self.resume_session_id:
            if self.cli == "claude":
                argv += ["--resume", self.resume_session_id]
            elif self.cli == "codex":
                # codex exec [OPTIONS] resume <SESSION_ID> [PROMPT]
                argv += ["resume", self.resume_session_id]
            elif self.cli == "opencode":
                argv += ["-s", self.resume_session_id]
            else:
                raise RuntimeError(f"{self.cli} 暂不支持会话续传，去掉 session 参数重试")

        via_stdin = self.cli_cfg.get("prompt_via", "arg") == "stdin"
        if not via_stdin:
            argv.append(self.prompt)

        if not os.path.isdir(self.cwd):
            raise RuntimeError(f"工作目录不存在: {self.cwd}")

        if self.use_worktree:
            self.worktree_path, self.worktree_branch = await prepare_worktree(self.cwd, self.task_id)
            self.cwd = self.worktree_path
            await on_event("status", f"已创建隔离 worktree: {self.worktree_path}（分支 {self.worktree_branch}）")

        env = dict(os.environ)
        env.setdefault("PYTHONIOENCODING", "utf-8")
        env["NB_TASK_ID"] = self.task_id  # 审批 MCP 服务用它定位任务
        self.proc = await asyncio.create_subprocess_exec(
            *argv,
            cwd=self.cwd,
            env=env,
            stdin=asyncio.subprocess.PIPE if via_stdin else asyncio.subprocess.DEVNULL,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        async def write_stdin():
            # 与读 stdout/stderr 并发写入，避免大 prompt 把管道写满、子进程又在等我们读输出造成死锁
            if not via_stdin:
                return
            try:
                self.proc.stdin.write(self.prompt.encode("utf-8"))
                await self.proc.stdin.drain()
            except (BrokenPipeError, ConnectionResetError):
                pass
            finally:
                try:
                    self.proc.stdin.close()
                except Exception:
                    pass

        extractor = EXTRACTORS.get(self.cli, lambda line: ("raw", line))
        result_text = []
        stderr_tail = []

        async def read_stdout():
            while True:
                line = await self.proc.stdout.readline()
                if not line:
                    break
                decoded = line.decode("utf-8", errors="replace").rstrip("\r\n")
                event = extractor(decoded)
                if event is None:
                    continue
                kind, text = event
                text = text[:OUTPUT_LINE_LIMIT]
                if kind == "session":
                    self.session_id = text
                    await on_event("status", "会话启动")
                elif kind == "result":
                    result_text.append(text)
                    await on_event("status", "任务收尾")
                else:
                    if kind == "text":
                        result_text.append(text)
                    await on_event(kind, text)

        async def read_stderr():
            while True:
                line = await self.proc.stderr.readline()
                if not line:
                    break
                stderr_tail.append(line.decode("utf-8", errors="replace").rstrip("\r\n"))
                if len(stderr_tail) > 50:
                    stderr_tail.pop(0)

        try:
            await asyncio.wait_for(
                asyncio.gather(write_stdin(), read_stdout(), read_stderr(), self.proc.wait()),
                timeout=self.timeout_sec,
            )
        except asyncio.TimeoutError:
            await _kill_tree(self.proc)
            raise RuntimeError(f"任务超时（{self.timeout_sec}s），已终止")
        finally:
            if self.proc.returncode is None:
                await _kill_tree(self.proc)

        duration_ms = int((time.monotonic() - start) * 1000)
        exit_code = self.proc.returncode
        if self.cancelled:
            raise RuntimeError("任务已被取消")

        final = result_text[-1] if result_text else ""
        if exit_code != 0 and not final:
            final = "\n".join(stderr_tail[-10:]) or f"CLI 退出码 {exit_code}"
        return exit_code, final, duration_ms
