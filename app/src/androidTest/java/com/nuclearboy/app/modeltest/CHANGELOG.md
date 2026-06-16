2026-06-15 1.1.61 ProviderLightweightConnectivityTest 所需调试模型参数改由 Gradle DSL 从本地未跟踪配置或 `NB_TEST_*` 环境变量注入，避免发布门禁命令行携带 API Key。
2026-06-14 1.1.27 ProviderLightweightConnectivityTest 支持从 nbBaseUrl/nbModel/nbApiKey/nbEndpointMode instrumentation 参数注入调试模型，适配 connected test 干净安装。
2026-06-14 1.1.25 新增 ProviderLightweightConnectivityTest，使用 App 当前自定义模型配置执行最小 ping，作为正式聊天前的设备端轻量连通性门禁。
