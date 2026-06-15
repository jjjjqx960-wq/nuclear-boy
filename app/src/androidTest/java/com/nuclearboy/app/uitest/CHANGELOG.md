2026-06-14 1.1.41 ToolLimitNoticeUiTest 改用模型自述无法访问文件和运行命令的英文消息注入真实前端，验证仍渲染工具受限卡。
2026-06-14 1.1.40 ToolLimitNoticeUiTest 改用英文工具受限助手消息注入真实前端，断言仍能渲染中文结构化工具受限卡。
2026-06-14 1.1.39 ToolMissingEvidenceReviewUiTest 断言真实前端结果复核卡显示 `tool.evidence.missing`、正式聊天链路口径和测试口径语义。
2026-06-14 1.1.38 ToolLimitNoticeUiTest 断言真实前端工具受限卡显示 `tool.protocol`、正式聊天链路口径和测试口径语义。
2026-06-14 1.1.37 SkillPromptGenerationJourneyTest 在首个写文件步骤遇到第三方工具协议降级时，接受明确“工具受限、未真实写入”的安全失败，不再把正确降级误判为产品失败。
2026-06-14 1.1.37 ChatFailureNoticeUiTest 断言真实前端失败卡显示“正式聊天 / stream=true”链路口径，并写入无障碍测试口径语义。
2026-06-14 1.1.36 ChatFailureNoticeUiTest 断言真实前端失败卡显示脱敏诊断指纹和 HTTP 状态，并把指纹写入语义描述。
2026-06-14 1.1.35 ChatFailureNoticeUiTest 增加 401 鉴权失败种子会话，断言真实前端显示 API Key 和重新测试正式聊天提示。
2026-06-14 1.1.34 新增 ChatFailureNoticeUiTest，通过 base64 debug 会话种子验证模型路由失败提示卡在真实前端可见。
2026-06-14 1.1.33 ToolMissingEvidenceReviewUiTest 扩展断言结构化结果复核卡，要求可见标题、风险摘要、无障碍语义和工具协议下一步。
2026-06-14 1.1.32 ToolMissingEvidenceReviewUiTest 改用非法 scheme 的快速失败调试模型，避免不可达 loopback 在设备上触发 120 秒网络超时。
2026-06-14 1.1.32 ChatJourneyRobot 的调试模型注入改用 base64 extras 传递 baseUrl、模型名和 Key，避免 `http://` 被 shell/广播拆坏导致正式聊天使用畸形 URL。
2026-06-14 1.1.32 ChatJourneyRobot 移除调试模型广播里的带空格 display name，避免受限设备 shell 拆参后影响 Receiver 投递。
2026-06-14 1.1.32 ChatJourneyRobot 的调试模型注入改为走目标 App debug-only Receiver，避免 test 进程写配置后目标前端仍使用旧模型。
2026-06-14 1.1.32 新增 ToolMissingEvidenceReviewUiTest，使用真实 App 输入、发送和不可达调试模型验证工具型请求完成后的复核提示可见。
2026-06-14 1.1.30 ToolDraftHintUiTest 增加真实点击发送后的系统证据提示断言，防止提示只停留在草稿态。
2026-06-14 1.1.29 ToolDraftHintUiTest 增加“追加防假执行提示”按钮点击和输入框回填断言。
2026-06-14 1.1.28 沉淀 Compose 输入态测试规则：发送前 UI 状态测试复用 ChatJourneyRobot.enterDraftText，不再裸用 UiObject2.setText 后直接断言。
2026-06-14 1.1.28 ChatJourneyRobot 暴露 enterDraftText，供发送前输入态测试复用真实聚焦、清空和设值路径。
2026-06-14 1.1.28 新增 ToolDraftHintUiTest，用真实 App 输入框验证第三方模型工具型草稿会在发送前出现工具能力预警。
2026-06-14 1.1.27 ChatJourneyRobot 启动目标 App 后会先处理常见系统权限弹窗，再执行前台门禁，避免 MIUI 授权页误拦正式聊天实测。
2026-06-14 1.1.27 ToolLimitNoticeUiTest 改为通过 debug-only Receiver 注入会话，规避受限 ROM 对 test 进程和 run-as 写 Android/data 的拦截。
2026-06-14 1.1.27 新增 ToolLimitNoticeUiTest，验证持久化的工具受限助手消息会在真实前端渲染提示卡和可定位语义。
2026-06-14 1.1.26 ChatJourneyRobot 收紧自报异常断言，避免把用户提示中的“没有问题”误判为模型自报“有问题”。
2026-06-14 1.1.26 SkillPromptGenerationJourneyTest 改为验证 workspace 真实写入路径，并在第三方工具降级时要求助手明确“工具受限、未写入”而非伪造成功。
2026-06-14 1.1.25 新增 SkillPromptGenerationJourneyTest，通过真实 App 对话生成测试 skill 与系统提示词，并读取设备项目文件验证落盘。
2026-06-14 1.1.25 ChatJourneyRobot 新增自检异常拦截，多轮最终轮出现“有问题/误报/存在不一致/自相矛盾”等自报异常时测试失败。
2026-06-14 1.1.25 抽取 ChatJourneyRobot 并新增多轮用户对话旅程测试，连续覆盖中文输入、工具请求、上下文回忆和最终错误状态确认。
2026-06-11 1.0.69 聊天旅程测试新增前台窗口门禁，root 输入和点击前必须确认目标 App 仍在前台，避免把操作打到系统设置页。
2026-06-11 1.0.69 聊天旅程测试改为 DPAD 聚焦输入框、发送后校验草稿清空，并把配置错误、空回复和请求格式错误作为正式发布硬失败。
2026-06-11 1.0.69 聊天旅程测试移除进程内 force-stop，改为宿主机清理进程并在输入框内用 root keyevent 清空残留内容。
2026-06-11 1.0.69 聊天旅程测试在 root 设备上使用 su -c input 执行真实点击输入，并在启动前强制清理 App 状态。
2026-06-11 1.0.69 聊天旅程测试在受限 ROM 上改用 shell input 执行点击和输入，规避 UIAutomator 注入权限拦截。
2026-06-11 1.0.69 聊天旅程测试改用真实输入框可见性作为启动门禁，并优先操作 EditText 提升设备端稳定性。
2026-06-11 1.0.69 聊天旅程测试固定 debug 目标包名并输出前台 Activity 状态，便于定位 ROM 前台识别问题。
2026-06-11 1.0.69 聊天旅程测试改用前台 Activity 状态判断启动结果，避免 UI 层包名匹配在部分 ROM 上误判。
2026-06-11 1.0.69 聊天旅程测试改用 shell am start 拉起 MainActivity，适配限制后台启动的 ROM。
2026-06-11 1.0.69 新增聊天发送用户旅程测试，覆盖输入、发送、等待回复和可见错误拦截。
