2026-06-14 1.1.23 DeepSeekApiClient 新增 testCustomProviderFormalChat，支持 OpenAI/Anthropic 正式 stream 探测、工具定义兼容检查和脱敏错误详情。
2026-06-14 1.1.23 目录规则补充第三方 SSE/JSON 可选块必须空安全解析，避免诊断解析器假设字段存在。
2026-06-12 1.0.70 sanitizeChatMessagesForProvider 对官方 DeepSeek 也剥离 reasoning_content，多轮思考模式不再触发 400。
2026-06-12 1.0.70 validateApiKey 改用 use 关闭响应修复连接泄漏；streamChat、streamAnthropicChat 和 checkBalance 不再把 CancellationException 当普通错误吞掉。
2026-06-11 1.0.68 自定义网关正式聊天请求增加 400/404/422 工具格式兼容重试，并清理历史消息中的 DeepSeek reasoning 扩展字段。
2026-06-11 1.0.58 将 OpenAI 兼容模型列表探测封装为公开 API，返回 endpoint、模型 id 列表和耗时。
2026-06-11 1.0.57 新增 OpenAI 兼容模型列表解析零件和 404 后 /v1/models 探测详情。
2026-06-11 1.0.56 新增第三方服务地址隐藏字符清理函数，并接入保存、读取、测试和 endpoint 构造路径。
