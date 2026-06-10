## 目录职责

`agent-core` 是部件层模块，负责 Agent 执行循环、工具注册、工具调用结果和模型工具定义转换。

## 边界

这里只承载 Agent 编排和工具抽象，不直接实现 UI、具体网络协议、文件系统工具细节或应用入口。

## 允许依赖

可以依赖 `common`、`api-deepseek`、`python-bridge`、`memory`、`skills`、`tools-docgen` 和 Kotlin 协程/序列化库。

## 禁止事项

不要在工具调用日志中输出明文 Token、账号密码、授权头或完整个人数据。不要从 Agent 反向依赖 `app` 或 UI 模块。

## 常用命令

- `./gradlew :agent-core:test`
- `./gradlew :agent-core:compileDebugKotlin`

## 验证方式

验证工具注册、工具执行、工具 schema 转换和 Agent 事件流。
