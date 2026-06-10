## 目录职责

`python-bridge` 是组件层模块，负责 Android 内 Python 执行桥、执行器门面和 Python 运行环境装配。

## 边界

这里只处理 Python 运行能力，不承载聊天 UI、Agent 业务目标或模型 API 协议。

## 允许依赖

可以依赖 `common`、协程、序列化和 AndroidX Core。不得依赖 `app` 或 UI 模块。

## 禁止事项

不要在错误、日志或测试输出中暴露账号密码、Token、密钥或完整个人数据。历史 `SandboxPolicy` 仅作为兼容工具保留，默认执行链不再启用隔离策略。

## 常用命令

- `./gradlew :python-bridge:test`
- `./gradlew :python-bridge:compileDebugKotlin`

## 验证方式

验证 Python 执行入口、脚本返回值、错误透传和历史策略工具的兼容行为。
