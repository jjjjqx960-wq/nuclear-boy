# remote-pc 模块

手机端连接电脑 nb-pc-bridge 守护进程的客户端模块，让核弹男孩的 AI 可以把任务下发给电脑上的 Claude Code / Codex 等编程 CLI。

## 零件清单

| 文件 | 职责 |
|------|------|
| `PcBridgeProtocol.kt` | WebSocket JSON 消息协议：出站 auth/run/cancel/ping 编码，入站 auth_ok/output/done/error 等解析（宽松解析，未知类型不崩） |
| `PcBridgeConfigStore.kt` | 连接配置加密存储（EncryptedSharedPreferences）：开关、地址、token（脱敏展示）、最近连接信息 |
| `PcBridgeClient.kt` | OkHttp WebSocket 客户端：testConnection 测试连接、runCliTask 执行任务并流式回传输出。每次操作独立连接，避免长连接复杂度 |

## 依赖关系

- 仅依赖 `:common`（AppResult/AppError），不依赖 agent-core——工具注册在 `:app` 的 AppModule 完成，避免模块环。
- 对端协议实现：`D:\tools\nb-pc-bridge`（电脑端 Python 守护进程），消息格式见其 `core/protocol.py`。

## 错误语义

- 未开启/未配置 → `InvalidRequest`，提示去设置页
- token 错误 → `ApiKeyInvalid`
- 连接超时 → `NetworkTimeout`，连接断开 → `NetworkUnavailable`
- 电脑端任务失败（CLI 缺失、超时、取消）→ `ServerError`，携带电脑端原始消息
