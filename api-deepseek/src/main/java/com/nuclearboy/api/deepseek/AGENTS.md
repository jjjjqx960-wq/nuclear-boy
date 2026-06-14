## 目录职责

`api-deepseek` 源码包承载模型服务协议、第三方网关配置、第三方网关兼容降级、endpoint 构造、模型名和服务地址清理、模型列表解析探测、正式聊天流式兼容探测、诊断提示、SSE 解析与 Token 统计。

## 边界

这里是组件层实现，只处理 API 协议和配置数据，不承载 Android 页面、Compose UI、Agent 编排或文件工具逻辑。

## 允许依赖

可以依赖 `common`、OkHttp、kotlinx serialization 和协程基础库。不得依赖 `app`、`ui-chat`、`ui-workspace` 或工具模块。

## 禁止事项

不要输出明文 API Key、Authorization 头、Token、签名密钥或个人数据。不要在这里实现 UI 文案展示和用户交互。

解析第三方 SSE/JSON 事件时，不要假设 `event`、`delta`、`content_block` 或 `tool_calls` 一定存在；所有可选块必须用 safe-call 或显式空值分支处理，避免新增诊断解析器后编译期/运行期空安全问题。

## 常用命令

- `./gradlew :api-deepseek:test`
- `./gradlew :api-deepseek:compileDebugKotlin`

## 验证方式

修改协议、第三方网关兼容降级、endpoint、模型列表探测、正式聊天流式兼容探测、模型名或服务地址清理逻辑后，必须运行 `:api-deepseek:test`，并在涉及 app 接入时补跑 `:app:testDebugUnitTest` 和 `:app:assembleDebug`。
