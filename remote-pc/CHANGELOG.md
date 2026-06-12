2026-06-12 1.0.87 屏幕缓冲模拟器：新增 TerminalEmulator（rows×cols 网格 + 光标，处理 CUP/CUU-D/ED/EL/IL/DL/ICH/DCH/ECH/滚动区 DECSTBM/SU-SD/DECSC-RC/备用屏 ?1049/47），renderWithScrollback 普通缓冲带 2000 行回滚、备用屏只显当前屏；ViewModel 改用 emulator.feed + 渲染按行 span。TerminalEmulatorTest 15 例。
2026-06-12 1.0.86 终端颜色解析：TerminalAnsi.parseSpans 解析 SGR（30-37/90-97/38;5;n/38;2;r;g;b/加粗/复位）产出带 ARGB 的 Span 列表，背景与光标/OSC 序列忽略；ViewModel 改存原始流由界面渲染。TerminalAnsiTest 新增 6 个颜色用例。
2026-06-12 1.0.85 远程终端协议+会话：PcBridgeProtocol 新增 term_open/term_input/term_resize/term_close 出站与 TermOutput/TermExit 入站；PcTerminalSession 用 callbackFlow 管理长连接（auth→term_open→流式 TermOutput，input/resize/close）；TerminalAnsi.strip 剥 CSI/OSC/退格得可读文本。三个新测试类覆盖协议、ANSI 清洗。
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
