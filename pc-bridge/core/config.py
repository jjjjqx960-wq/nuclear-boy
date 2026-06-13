# -*- coding: utf-8 -*-
"""配置管理：config.json 的加载、初始化和 token 生成。"""
import hashlib
import json
import secrets
from pathlib import Path

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config.json"

DEFAULT_CONFIG = {
    "host": "0.0.0.0",
    "port": 7860,
    "token": "",
    "default_cwd": "D:\\",
    "max_concurrent_tasks": 2,
    "task_timeout_sec": 900,
    "clis": {
        "claude": {
            "command": "claude",
            "args": ["-p", "--output-format", "stream-json", "--verbose",
                     "--permission-mode", "acceptEdits"],
            "prompt_via": "stdin",
        },
        "codex": {
            "command": "codex",
            "args": ["exec", "--json", "--skip-git-repo-check"],
            "prompt_via": "arg",
        },
        "opencode": {
            "command": "opencode",
            "args": ["run", "--format", "json"],
            "prompt_via": "arg",
        },
    },
}


def generate_token() -> str:
    """生成 32 字节十六进制随机 token。"""
    return secrets.token_hex(32)


def default_room(token: str) -> str:
    """中继默认 room：取 token 的 SHA-256 指纹前 24 位。

    用哈希而非 token 前缀，使 room（会出现在中继 URL 路径里、对中继运营方可见）
    不泄露 token 的任何明文片段，同时保持唯一且不可反推。
    """
    return hashlib.sha256(token.encode("utf-8")).hexdigest()[:24]


def load_config(path: Path = CONFIG_PATH) -> dict:
    """读取配置；不存在时抛 FileNotFoundError，提示先 init。"""
    if not path.exists():
        raise FileNotFoundError(
            f"配置不存在: {path}，先运行 python bridge.py init"
        )
    with open(path, "r", encoding="utf-8") as f:
        cfg = json.load(f)
    if not cfg.get("token"):
        raise ValueError("config.json 缺少 token，运行 python bridge.py init --rotate-token")
    # 非破坏性合并：默认配置里新增的 CLI 自动补进旧配置
    for name, default_cli in DEFAULT_CONFIG["clis"].items():
        cfg.setdefault("clis", {}).setdefault(name, dict(default_cli))
    return cfg


def save_config(cfg: dict, path: Path = CONFIG_PATH) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)


def init_config(rotate_token: bool = False, path: Path = CONFIG_PATH) -> dict:
    """初始化配置文件；已存在则只在需要时补 token。"""
    if path.exists():
        with open(path, "r", encoding="utf-8") as f:
            cfg = json.load(f)
    else:
        cfg = json.loads(json.dumps(DEFAULT_CONFIG))
    if rotate_token or not cfg.get("token"):
        cfg["token"] = generate_token()
    save_config(cfg, path)
    return cfg
