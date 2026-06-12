2026-06-12 1.0.83 关闭码翻译：PcBridgeClient.friendlyCloseMessage 把中继/鉴权关闭码（4003/4004/4009/4000/4029）映射成可操作中文，onClosed 用它替换原英文 reason；PcBridgeCloseMessageTest 覆盖各码与回退。
2026-06-12 1.0.82 配对载荷：新增 PcPairingPayload.parse 解析 nbpair://pair?u=&t= 二维码（纯 JVM 不依赖 android.net.Uri，URLDecoder 还原内嵌 query），扫码后回填 url+token；PcPairingPayloadTest 覆盖局域网/中继/非法/空白用例。
2026-06-12 1.0.81 公网中继接入：手机地址直接填中继 client URL（ws://服务器:8970/client/<room>?key=口令）即走中继，toHttpWsUrl 透传 path+query 复用既有连接逻辑无需改协议；设置页新增外网控制说明。
2026-06-12 1.0.80 权限审批：RunMessage.approval=ask、permission_request 入站、permission_response 出站；PermissionPromptBus 驱动聊天界面审批弹窗。
2026-06-12 1.0.78 session="last" 别名自动续传；配置存储记录各 CLI 最近会话 ID。
2026-06-12 1.0.77 RunMessage.worktree 隔离标记、Done.worktreePath/worktreeBranch 回传；runCliTask 新增 useWorktree 参数。
2026-06-12 1.0.76 新增 listRunningTasks 任务列表查询和 cancelActiveTasksAsync 批量取消；客户端跟踪本机发起的在途任务。
2026-06-12 1.0.74 断线自动恢复：runCliTask 网络波动后重连（最多 5 次），用 get_result 取回结果或重挂输出流，避免任务白跑。
2026-06-12 1.0.73 协议和客户端支持会话续传：RunMessage.sessionId 下发续传、Done.sessionId 回传本次会话。
2026-06-12 1.0.72 新增 remote-pc 模块：WebSocket 协议、加密配置存储、桥接客户端，支持手机端把任务下发给电脑上的 Claude Code / Codex。
