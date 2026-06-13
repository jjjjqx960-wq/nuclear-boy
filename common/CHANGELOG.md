2026-06-12 1.1.6 新增 AppSettingsStore（SharedPreferences），承载自定义指令。
2026-06-12 1.1.5 新增 SharedIntentBus（replay=1 SharedFlow，分享文本投递），3 单测。
2026-06-12 1.1.0 新增 TokenUsageFormat.inline：↑输入↓输出+缓存命中，无拆分回退总量，纯函数 5 单测。
2026-06-12 1.0.98 ChatEditing.removeMessage：按 id 删单条消息，纯函数 2 单测。
2026-06-12 1.0.97 新增 MessageSearch.find/count：会话内大小写不敏感子串匹配（跳过系统消息），返回命中下标，纯函数 6 单测。
2026-06-12 1.0.96 新增 ChatEditing.prepareEdit：截断到目标用户消息之前并取回内容，纯函数 5 单测。
2026-06-12 1.0.95 新增 ConversationExporter：ChatMessage 列表导出为 Markdown（分段/工具压行/跳过系统空消息/标题日期），纯函数 7 单测。
2026-06-12 1.0.75 新增 ToolProgressBus 工具进度总线（SharedFlow，非阻塞 tryEmit，行截断 300 字符）。
2026-06-10 1.0.2 将第三方网关常见的 404、405、422 映射为请求配置错误，避免路径/方法不匹配时被当作可重试未知错误。
2026-06-10 1.0.2 补齐模块单元测试 JUnit 依赖，恢复通用零件测试编译入口，并扩展问题类文案的情绪标记。
2026-06-10 1.0.2 同步 Python 非隔离执行语义，更新通用错误与常量说明文案。
