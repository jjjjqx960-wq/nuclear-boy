## 目录职责

`api-deepseek` 是组件层模块，负责模型 API DTO、API Key 管理、DeepSeek/OpenAI 兼容请求、流式 SSE 解析、Token 统计和上下文预算。

## 边界

这里只处理模型服务协议和 API 配置，不承载 UI、文件系统工具、Android 页面或 Agent 编排策略。

## 允许依赖

可以依赖 `common` 和网络/序列化基础库。不得依赖 `app`、`ui-chat`、`agent-core` 或工具模块。

## 禁止事项

不要在日志中输出明文 API Key、Token、请求授权头或完整个人数据。不要默认跳过 TLS 或主机名校验。

## 常用命令

- `./gradlew :api-deepseek:test`
- `./gradlew :api-deepseek:compileDebugKotlin`

## 验证方式

检查 DTO 序列化、SSE 事件解析、Token 统计和第三方 OpenAI 兼容网关请求体。
