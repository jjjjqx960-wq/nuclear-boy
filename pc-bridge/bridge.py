# -*- coding: utf-8 -*-
"""nb-pc-bridge — 核弹男孩手机端控制电脑编程 CLI 的桥接守护进程。

用法:
  python bridge.py init [--rotate-token]   初始化配置并生成 token
  python bridge.py serve                   启动 WebSocket 服务
  python bridge.py status                  显示配置、本机 IP 和 CLI 探测结果
  python bridge.py selftest --cli claude --prompt "1+1=?"   本机自测一条任务
"""
import argparse
import asyncio
import json
import logging
import socket
import sys
import uuid
from pathlib import Path

from core.config import init_config, load_config, default_room, CONFIG_PATH
from core.runners import CliTask, detect_cli_versions
from core.server import BridgeServer


def lan_ips():
    """列出本机非回环 IPv4 地址，供手机端填写。"""
    ips = set()
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            ip = info[4][0]
            if not ip.startswith("127."):
                ips.add(ip)
    except socket.gaierror:
        pass
    return sorted(ips)


def print_qr(uri):
    """在终端打印二维码（优先 qrcode 库，缺失时只打印文本）。"""
    try:
        import qrcode
        qr = qrcode.QRCode(border=1)
        qr.add_data(uri)
        qr.make(fit=True)
        qr.print_ascii(invert=True)
    except Exception:
        print("  (未装 qrcode 库，无法画二维码，请手动填写下面地址)")
    print(f"  配对串: {uri}")


def cmd_pair(args):
    """打印配对二维码：手机扫码即可自动填入地址和 token。"""
    from core.pairing import make_pair_uri
    cfg = load_config()
    token = cfg["token"]
    if args.relay:
        relay_key = args.relay_key or cfg.get("relay_key", "")
        room = args.room or cfg.get("relay_room") or default_room(token)
        client_base = args.relay.rstrip("/").replace("/agent", "")
        key_q = f"?key={relay_key}" if relay_key else ""
        url = f"{client_base}/client/{room}{key_q}"
        print(f"[公网中继配对] room={room}")
    else:
        ips = lan_ips()
        host = args.host or (ips[0] if ips else "127.0.0.1")
        url = f"ws://{host}:{cfg['port']}"
        print(f"[局域网配对] {url}")
    print_qr(make_pair_uri(url, token))


def cmd_init(args):
    cfg = init_config(rotate_token=args.rotate_token)
    print(f"[OK] 配置已写入 {CONFIG_PATH}")
    print(f"  token: {cfg['token']}")
    print(f"  端口: {cfg['port']}")
    print("  手机端可填地址:")
    for ip in lan_ips():
        print(f"    ws://{ip}:{cfg['port']}")


def cmd_status(args):
    cfg = load_config()
    print(f"配置: {CONFIG_PATH}")
    print(f"端口: {cfg['port']}  默认工作目录: {cfg['default_cwd']}")
    print(f"token 指纹: {cfg['token'][:8]}...{cfg['token'][-4:]}")
    print("本机地址:")
    for ip in lan_ips():
        print(f"  ws://{ip}:{cfg['port']}")
    print("CLI 探测:")
    for name, ver in detect_cli_versions(cfg["clis"]).items():
        print(f"  {name}: {ver}")


def cmd_serve(args):
    handlers = [logging.StreamHandler(sys.stdout)]
    if args.log_file:
        handlers.append(logging.FileHandler(args.log_file, encoding="utf-8"))
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        handlers=handlers,
    )
    cfg = load_config()
    server = BridgeServer(cfg)

    relay_url = args.relay or cfg.get("relay_url", "")
    if relay_url:
        from core.relay_client import run_relay_agent
        # room 默认用 token 指纹，保证唯一且不可猜
        room = args.room or cfg.get("relay_room") or default_room(cfg["token"])
        relay_key = args.relay_key or cfg.get("relay_key", "")

        client_base = relay_url.rstrip("/").replace("/agent", "")
        key_q = f"?key={relay_key}" if relay_key else ""
        logging.info("中继 room=%s", room)
        logging.info("手机填地址: %s/client/%s%s", client_base, room, key_q)

        async def run_both():
            await asyncio.gather(
                server.serve_forever(),
                run_relay_agent(server, relay_url, room, relay_key),
            )
        asyncio.run(run_both())
    else:
        asyncio.run(server.serve_forever())


def cmd_install_autostart(args):
    """注册 Windows 登录自启计划任务，pythonw 后台运行并写日志文件。"""
    import subprocess
    bridge_path = Path(__file__).resolve()
    pythonw = Path(sys.executable).parent / "pythonw.exe"
    runner = str(pythonw if pythonw.exists() else sys.executable)
    log_path = bridge_path.parent / "bridge.log"
    command = f'"{runner}" "{bridge_path}" serve --log-file "{log_path}"'
    result = subprocess.run(
        ["schtasks", "/Create", "/F", "/TN", "nb-pc-bridge",
         "/TR", command, "/SC", "ONLOGON", "/RL", "LIMITED"],
        capture_output=True, text=True, encoding="gbk", errors="replace",
    )
    if result.returncode == 0:
        print("[OK] 已注册登录自启计划任务 nb-pc-bridge")
        print(f"  命令: {command}")
        print(f"  日志: {log_path}")
        print("  立即启动: schtasks /Run /TN nb-pc-bridge")
    else:
        print(f"[FAIL] 注册失败: {result.stderr or result.stdout}")
        sys.exit(1)


def cmd_clean_worktrees(args):
    """清理某仓库由本桥接创建的 nb/ worktree 和分支。"""
    import subprocess
    repo = args.repo
    listing = subprocess.run(
        ["git", "-C", repo, "worktree", "list", "--porcelain"],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if listing.returncode != 0:
        print(f"[FAIL] 不是 git 仓库: {repo}")
        sys.exit(1)
    removed = 0
    current = {}
    entries = []
    for line in listing.stdout.splitlines():
        if line.startswith("worktree "):
            current = {"path": line.split(" ", 1)[1]}
            entries.append(current)
        elif line.startswith("branch "):
            current["branch"] = line.split(" ", 1)[1]
    for entry in entries:
        branch = entry.get("branch", "")
        if "/nb/" not in branch and not branch.startswith("refs/heads/nb/"):
            continue
        subprocess.run(["git", "-C", repo, "worktree", "remove", "--force", entry["path"]],
                       capture_output=True)
        short_branch = branch.replace("refs/heads/", "")
        subprocess.run(["git", "-C", repo, "branch", "-D", short_branch], capture_output=True)
        print(f"  removed {entry['path']} ({short_branch})")
        removed += 1
    print(f"[OK] 清理了 {removed} 个 nb worktree")


def cmd_uninstall_autostart(args):
    import subprocess
    result = subprocess.run(
        ["schtasks", "/Delete", "/F", "/TN", "nb-pc-bridge"],
        capture_output=True, text=True, encoding="gbk", errors="replace",
    )
    if result.returncode == 0:
        print("[OK] 已删除自启计划任务 nb-pc-bridge")
    else:
        print(f"[FAIL] 删除失败: {result.stderr or result.stdout}")
        sys.exit(1)


def cmd_selftest(args):
    cfg = load_config()
    if args.cli not in cfg["clis"]:
        print(f"[FAIL] 不支持的 CLI: {args.cli}")
        sys.exit(1)

    async def go():
        task = CliTask(
            task_id=uuid.uuid4().hex,
            cli=args.cli,
            prompt=args.prompt,
            cwd=args.cwd or cfg["default_cwd"],
            timeout_sec=args.timeout,
            cli_cfg=cfg["clis"][args.cli],
            resume_session_id=args.session or "",
        )

        async def on_event(kind, text):
            print(f"  [{kind}] {text[:200]}")

        exit_code, result, duration_ms = await task.run(on_event)
        print(json.dumps(
            {"exitCode": exit_code, "result": result[:500], "durationMs": duration_ms,
             "sessionId": task.session_id},
            ensure_ascii=False, indent=2,
        ))

    asyncio.run(go())


def main():
    parser = argparse.ArgumentParser(description="核弹男孩 PC 桥接服务")
    sub = parser.add_subparsers(dest="command", required=True)

    p_init = sub.add_parser("init", help="初始化配置并生成 token")
    p_init.add_argument("--rotate-token", action="store_true", help="重新生成 token")
    p_init.set_defaults(func=cmd_init)

    p_status = sub.add_parser("status", help="显示配置和 CLI 探测结果")
    p_status.set_defaults(func=cmd_status)

    p_serve = sub.add_parser("serve", help="启动 WebSocket 服务")
    p_serve.add_argument("--log-file", default=None, help="同时把日志写入文件")
    p_serve.add_argument("--relay", default=None, help="同时反连公网中继，如 ws://1.2.3.4:8970")
    p_serve.add_argument("--room", default=None, help="中继 room（默认用 token 指纹）")
    p_serve.add_argument("--relay-key", default=None, help="中继准入口令")
    p_serve.set_defaults(func=cmd_serve)

    p_pair = sub.add_parser("pair", help="打印配对二维码（手机扫码自动填地址+token）")
    p_pair.add_argument("--host", default=None, help="指定局域网 IP（默认取第一个）")
    p_pair.add_argument("--relay", default=None, help="生成公网中继配对，如 ws://1.2.3.4:8970")
    p_pair.add_argument("--room", default=None, help="中继 room（默认用 token 指纹）")
    p_pair.add_argument("--relay-key", default=None, help="中继准入口令")
    p_pair.set_defaults(func=cmd_pair)

    p_auto = sub.add_parser("install-autostart", help="注册 Windows 登录自启计划任务")
    p_auto.set_defaults(func=cmd_install_autostart)

    p_unauto = sub.add_parser("uninstall-autostart", help="删除自启计划任务")
    p_unauto.set_defaults(func=cmd_uninstall_autostart)

    p_clean = sub.add_parser("clean-worktrees", help="清理仓库的 nb/ 隔离 worktree")
    p_clean.add_argument("--repo", required=True, help="仓库路径")
    p_clean.set_defaults(func=cmd_clean_worktrees)

    p_test = sub.add_parser("selftest", help="本机自测一条 CLI 任务")
    p_test.add_argument("--cli", required=True, choices=["claude", "codex", "opencode"])
    p_test.add_argument("--prompt", required=True)
    p_test.add_argument("--cwd", default=None)
    p_test.add_argument("--timeout", type=int, default=300)
    p_test.add_argument("--session", default=None, help="续传已有 claude 会话 ID")
    p_test.set_defaults(func=cmd_selftest)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
