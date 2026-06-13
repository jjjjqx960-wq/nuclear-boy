2026-06-12 1.1.1 质量加固：TerminalEmulator.resize 同步 resize altGrid（修 TUI 改大小退出花屏）+ putChar.clearWideCharAt 清宽字符残留续格；server.py _EncryptedWs.recv 非法帧转 ConnectionClosed；PcBridgeProtocol.extractEnc 正规 JSON 解析替换 substring（PcBridgeClient/PcTerminalSession 共用）；PcCrypto SecureRandom 单例。TerminalEmulatorTest +2。
2026-06-12 1.0.94 会话列表：协议加 list_sessions 出站与 SessionsList 入站（SessionInfo）；PcBridgeClient.listSessions。PcBridgeProtocolTest +2 例。
2026-06-12 1.0.93 增量同步：Output 加 seq、get_result 加 sinceSeq；runCliTask 跟踪 maxSeq、重连请求 sinceSeq=maxSeq+1 并按 seq 去重。PcBridgeProtocolTest +3 例（seq 解析、缺省回退、sinceSeq 编码）。
2026-06-12 1.0.92 通道加密：新增 PcCrypto（SHA-256 派生密钥 + AES-256-GCM，java.util.Base64）；PcBridgeClient/PcTerminalSession 开启加密时握手发 {"type":"auth"} 信封、收发走 envelope/解密；配置加 isEncryptionEnabled/setEncryptionEnabled + PcBridgeConfig.encrypted。PcCryptoTest 6 例（含与 Python 字节级一致的固定向量）。
2026-06-12 1.0.91 写文件协议：协议加 write_file 出站与 FileWritten 入站；PcBridgeClient.writeFile（覆盖/追加）。PcBridgeProtocolTest +2 例。审批在 :app 的 pc_write_file 工具里走 PermissionPromptBus，client 仅负责传输。
2026-06-12 1.0.90 只读文件浏览：协议加 list_dir/read_file 出站与 DirListing/FileContent 入站；PcBridgeClient.listDir/readFile（withSession 请求-响应）。PcBridgeProtocolTest +3 例（编码、目录列表、文件内容解析）。
2026-06-12 1.0.89 CJK 双宽字符：新增 CharWidth.of（东亚宽度判定，CJK/全角=2、组合记号=0）；TerminalEmulator.putChar 按宽度推进光标、宽字符占两格（续格 continuation 标记，渲染跳过），放不下时换行。CharWidthTest 6 例 + 模拟器 CJK 对齐/换行 2 例。
2026-06-12 1.0.88 特殊键 + 尺寸自适应：TerminalKeys（方向/翻页/Home-End/Ctrl 组合/Esc-Tab-Del → xterm 序列，借鉴 ReTerminal）、TerminalGeometry（像素+字符尺寸算 cols/rows，带钳制与回退）；ViewModel 新增 sendKey/onResize（resize emulator+session）。TerminalKeysGeometryTest 7 例。
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
