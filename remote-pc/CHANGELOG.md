2026-06-12 1.0.76 新增 listRunningTasks 任务列表查询和 cancelActiveTasksAsync 批量取消；客户端跟踪本机发起的在途任务。
2026-06-12 1.0.74 断线自动恢复：runCliTask 网络波动后重连（最多 5 次），用 get_result 取回结果或重挂输出流，避免任务白跑。
2026-06-12 1.0.73 协议和客户端支持会话续传：RunMessage.sessionId 下发续传、Done.sessionId 回传本次会话。
2026-06-12 1.0.72 新增 remote-pc 模块：WebSocket 协议、加密配置存储、桥接客户端，支持手机端把任务下发给电脑上的 Claude Code / Codex。
