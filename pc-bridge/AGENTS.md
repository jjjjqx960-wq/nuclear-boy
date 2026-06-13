# nb-pc-bridge

核弹男孩（Nuclear Boy）手机端控制电脑编程 CLI 的桥接守护进程。手机经局域网/USB 共享网络连入，AI 通过它把任务下发给电脑上的 claude / codex。

## 结构

```
nb-pc-bridge/
├── bridge.py        # CLI 入口: init / status / serve / selftest
├── config.json      # 端口、token、CLI 命令配置（init 生成，含密钥不入库）
├── core/
│   ├── config.py    # 配置加载/初始化/token 生成
│   ├── protocol.py  # WebSocket JSON 消息协议（消息格式见文件头注释）
│   ├── runners.py   # CLI 子进程执行器，流式解析 claude stream-json / codex --json
│   └── server.py    # websockets 服务端：鉴权、分发、流式回传
└── tests/
    └── test_ws_client.py  # 端到端测试客户端
```

## 用法

```bash
python bridge.py init           # 生成 config.json + token
python bridge.py status         # 看本机 IP、token 指纹、CLI 探测
python bridge.py serve          # 启动服务（默认 0.0.0.0:7860）
python bridge.py selftest --cli claude --prompt "1+1=?"   # 不经网络直测执行器
python tests/test_ws_client.py --cli codex --prompt "..."  # 全链路测试
python tests/test_ws_client.py --bad-token                 # 鉴权拒绝测试
```

## 实测记录

- 2026-06-12 远程终端（ConPTY）：core/terminal.py 用 pywinpty(3.0.5, ConPTY) 起真伪终端，阻塞 read 走 asyncio.to_thread 不卡事件循环；协议 term_open/term_input/term_resize/term_close + term_output/term_exit；server.py 按 term_id 管理会话、连接断开即关其终端（交互式不保活，区别于后台任务）。tests/test_terminal.py 经 WS 全链路（开 cmd.exe→键入 echo→收输出→resize→关闭清理）通过。依赖：pip install pywinpty。借鉴 claudecodeui(node-pty)/claude-remote-terminal(Unix pty+tmux，仅 Mac/Linux 我们改 Windows ConPTY)。

- 2026-06-12 默认 room 哈希化（安全）：原 room=token[:24] 直接泄露 token 前 24 位明文给中继运营方/URL 观察者，改 core.config.default_room()=sha256(token)[:24]（serve/pair 都用，QR 里 t= 仍是完整 token 只给可信手机看）。剩余 40 hex 本就不可暴破，但 room 不再泄露任何明文。tests/test_default_room.py 验证非前缀/确定性/不同 token 不同 room。

- 2026-06-12 中继关闭透传：_VirtualWs.close 把 server 主动关闭（4029 鉴权封禁等）发 {"_cid","_event":"server_close","_code","_reason"} 给中继，relay_server agent 循环识别后带码 close 对应手机并清理 client 条目。否则手机收不到关闭码只能等超时。tests/test_relay_server_close.py 验证手机端收到 4029。注意：bridge auth-fail 走 4003 但先发 auth_fail 消息再 close，消息赢 race 手机显示 token 提示不会误报"中继口令错"。**已知安全弱点**：中继下 _VirtualWs.remote_address 每 cid 不同（relay:room:cid），导致 bridge 按 IP 的 5 次封禁在中继侧被绕过（每次重连新 cid=新"IP"）——直连 LAN 不受影响；修需把中继虚拟连接的封禁桶改按 room 维度（但要防单攻击者 DoS 锁死合法手机，且 room=token 指纹+relay_key 已是双重门槛，暂记为待办）。

- 2026-06-12 扫码配对上线：core/pairing.py（make/parse_pair_uri，nbpair://pair?u=&t= URI，url 含中继 query 时整体 percent-encode）+ bridge.py pair 子命令（qrcode 终端 ASCII 码，支持 --relay/--room/--relay-key）。手机端 PcPairingPayload.parse 对应解码，扫码回填 url+token。tests/test_pairing.py + 安卓 PcPairingPayloadTest 双侧通过。相机识别需对屏人工确认。

- 2026-06-12 公网中继上线：relay/relay_server.py（room 配对 + cid 多手机分流透传，/agent/<room> 挂电脑、/client/<room> 接手机，relay_key 准入）；core/relay_client.py 用 _VirtualWs 把单条 agent 连接 demux 成多个虚拟连接喂 handle_client，复用全部鉴权/任务/审批逻辑零改动，断线 5s 自动重连；bridge serve --relay/--room/--relay-key，启动打印 room 和手机填写地址。中继不解析业务、token 端到端校验。全链路（建会话+续传+错误 token 拒绝）+ 双手机并发 cid demux 无串台实测通过。

- 2026-06-12 权限转发上线：approval=ask 时 claude 加 --permission-prompt-tool mcp__nbapproval__approve + --mcp-config + --strict-mcp-config + --settings(空 allow)；mcp_approval.py 经 bridge permission_request/permission_response 转发手机审批；tests/test_permission_relay.py 批准/拒绝双路径通过。注意：echo 等被 claude 判安全的命令不触发审批，写文件/危险命令会触发；Windows asyncio 不能 connect_read_pipe(stdin)，MCP 服务用同步 readline。

- 2026-06-12 OpenCode 接入：run --format json 事件流（text/step_start 取 sessionID），resume 走 -s；本机建会话+续传、真机远程任务实测通过。注意 opencode 默认模型由 ~/.config/opencode/opencode.json 决定，zen 免费模型可能限流。

- 2026-06-12 鉴权限速上线：同一来源 5 次失败封禁 300s（code 4029），成功鉴权清零计数；实测第 6 次起连正确 token 也被拒。

- 2026-06-12 worktree 隔离上线：run 消息 worktree=true 在仓库旁建 .nb-worktrees/<repo>-<id8> + nb/<id8> 分支执行，done 回传路径；clean-worktrees 命令清理；tests/test_worktree.py 实测主仓库零污染；3 并发任务压测通过。

- 2026-06-12 任务管理上线：list_tasks 列正在执行任务（id/cli/时长/摘要/目录）、cancel 返回 cancelled 确认并以"任务已被取消"错误收尾；install-autostart 注册登录自启计划任务（pythonw + bridge.log）；tests/test_cancel_and_list.py 全路径通过。

- 2026-06-12 断线恢复上线：客户端断线只断流不杀任务，done/error 结果缓存最近 100 条；新增 get_result 消息（任务进行中重挂输出流，已完成取回缓存结果，未知任务报错）；tests/test_disconnect_resume.py 全路径实测通过。
- 2026-06-12 codex 续传上线：捕获 thread.started.thread_id，resume 走 `codex exec [opts] resume <id> <prompt>`；本机和真机实测通过。

- 2026-06-12 会话续传上线：run 消息支持 sessionId（claude --resume），done 消息回传本次会话 ID；本机和真机实测跨任务记忆通过。
- 2026-06-12 初版全链路通过：claude 2.1.175（stream-json，8.7s）、codex 0.139.0（--json，9.3s）、错误 token 拒绝、ping/pong、WS 流式回传。

## 注意

- claude 经 npm .cmd 包装，必须 `cmd /c` 启动；prompt 走 stdin 避免 cmd 引号转义问题。
- codex 是原生 exe，prompt 直接作为 argv 传入。
- 超时/取消用 `taskkill /T /F` 杀整棵进程树，避免孤儿进程。
- claude 默认 `--permission-mode acceptEdits`（config.json 可改）。
- token 为 32 字节 hex，鉴权用 hmac.compare_digest 防时序攻击。
