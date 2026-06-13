# nb-pc-bridge — 核弹男孩电脑端桥接

手机端核弹男孩通过它控制电脑上的 AI 编程 CLI（Claude Code / Codex / OpenCode）、远程终端、读写文件等。手机 ↔ 本桥接 ↔ 本机 CLI/终端。

## 快速开始

```bash
pip install -r requirements.txt          # 依赖：websockets / pywinpty(远程终端) / qrcode(扫码配对) / cryptography(加密)
python bridge.py init                     # 生成 token，打印手机可填的 ws:// 地址
python bridge.py serve                    # 启动服务（局域网/USB 同网即可连）
```

手机：设置 → 远程电脑 → 填地址+token，或 `python bridge.py pair` 打印二维码扫码配对。

## 常用命令

| 命令 | 用途 |
|------|------|
| `python bridge.py init [--rotate-token]` | 初始化/换 token |
| `python bridge.py status` | 看配置、本机 IP、CLI 探测 |
| `python bridge.py serve [--log-file F] [--relay ws://中继:8970 --relay-key 口令]` | 启动（可反连公网中继外网控制）|
| `python bridge.py pair [--relay ... --relay-key ...]` | 打印配对二维码 |
| `python bridge.py install-autostart` | 注册登录自启 |
| `python bridge.py clean-worktrees --repo <仓库>` | 清理隔离 worktree |
| `python bridge.py selftest --cli claude --prompt "1+1"` | 本机自测一条任务 |
| `python relay/relay_server.py --port 8970 --key 口令` | 在公网服务器上跑中继 |

## 安全

- `config.json`（含 token）由 `init` 生成，**不要提交**（已在仓库 .gitignore 忽略）。
- 走公网中继时在手机端开启「端到端加密」：AES-256-GCM 加密每条消息，中继只见密文、拿不到 token。

## 测试

```bash
python tests/test_ws_client.py        # 全链路
python tests/test_crypto.py           # 通道加密（含与安卓字节级互通向量）
python tests/test_terminal.py         # 远程终端(ConPTY)
# 其余见 tests/ 目录
```

详见 `AGENTS.md`（架构与实测记录）。
