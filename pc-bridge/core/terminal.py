# -*- coding: utf-8 -*-
"""Windows ConPTY 终端会话：手机端"远程终端界面"的电脑侧后端。

借鉴 claudecodeui（node-pty）/ claude-remote-terminal（Unix pty）的终端形态，
在 Windows 上用 pywinpty(ConPTY) 起一个真伪终端，把输出流式回传手机、
接收手机键入、支持改窗口大小——这样能跑交互式程序（含直接跑 claude/codex 的
交互模式），补上"工具调用之外的远程终端"形态。

阻塞式 pty.read 用 asyncio.to_thread 包起来，不卡事件循环（同 claude-remote-terminal 的做法）。
"""
import asyncio
import logging

log = logging.getLogger("nb-bridge")

DEFAULT_COLS = 80
DEFAULT_ROWS = 24
READ_CHUNK = 4096
# 默认起 PowerShell（比 cmd 体验好），缺失回退 cmd
DEFAULT_SHELL_CANDIDATES = ["powershell.exe", "cmd.exe"]


def _resolve_shell(cmd: str) -> str:
    import shutil
    if cmd:
        return cmd
    for cand in DEFAULT_SHELL_CANDIDATES:
        if shutil.which(cand):
            return cand
    return "cmd.exe"


class TerminalSession:
    """一个 ConPTY 会话。start() 后通过回调流式吐输出，结束时回调退出码。"""

    def __init__(self, term_id: str, cols: int, rows: int, cwd: str = None, cmd: str = ""):
        self.term_id = term_id
        self.cols = max(1, int(cols or DEFAULT_COLS))
        self.rows = max(1, int(rows or DEFAULT_ROWS))
        self.cwd = cwd or None
        self.cmd = _resolve_shell(cmd)
        self._pty = None
        self._pump_task = None
        self._closed = False

    def start(self, on_output, on_exit):
        """起 pty 并开始泵输出。on_output(data:str) / on_exit(code:int) 为 async 回调。"""
        from winpty import PtyProcess
        self._pty = PtyProcess.spawn(
            self.cmd, cwd=self.cwd, dimensions=(self.rows, self.cols)
        )
        self._on_output = on_output
        self._on_exit = on_exit
        self._pump_task = asyncio.ensure_future(self._pump())
        log.info("终端开启 id=%s shell=%s %dx%d", self.term_id, self.cmd, self.cols, self.rows)

    async def _pump(self):
        try:
            while not self._closed:
                try:
                    data = await asyncio.to_thread(self._pty.read, READ_CHUNK)
                except EOFError:
                    break
                except (OSError, Exception):
                    break
                if data:
                    await self._on_output(data)
        finally:
            code = 0
            try:
                if self._pty is not None and not self._pty.isalive():
                    code = self._pty.exitstatus or 0
            except Exception:
                pass
            if not self._closed:
                await self._on_exit(code)
            log.info("终端结束 id=%s code=%s", self.term_id, code)

    async def write(self, data: str):
        if self._closed or self._pty is None:
            return
        try:
            await asyncio.to_thread(self._pty.write, data)
        except Exception as exc:
            log.warning("终端写入失败 id=%s: %s", self.term_id, exc)

    def resize(self, cols: int, rows: int):
        self.cols = max(1, int(cols or self.cols))
        self.rows = max(1, int(rows or self.rows))
        if self._pty is not None and not self._closed:
            try:
                self._pty.setwinsize(self.rows, self.cols)
            except Exception as exc:
                log.warning("终端调整大小失败 id=%s: %s", self.term_id, exc)

    async def close(self):
        if self._closed:
            return
        self._closed = True
        if self._pty is not None:
            try:
                self._pty.close(force=True)
            except Exception:
                pass
        if self._pump_task is not None:
            self._pump_task.cancel()
        log.info("终端关闭 id=%s", self.term_id)


class TerminalManager:
    """按 term_id 管理多个终端会话。"""

    def __init__(self):
        self._sessions = {}

    def get(self, term_id: str):
        return self._sessions.get(term_id)

    def add(self, session: TerminalSession):
        self._sessions[session.term_id] = session

    async def close(self, term_id: str):
        session = self._sessions.pop(term_id, None)
        if session is not None:
            await session.close()

    async def close_all(self):
        for session in list(self._sessions.values()):
            await session.close()
        self._sessions.clear()

    def remove(self, term_id: str):
        self._sessions.pop(term_id, None)
