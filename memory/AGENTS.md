## 目录职责

`memory` 是组件层模块，负责记忆数据模型、Room 存储、记忆检索和上下文拼接相关能力。

## 边界

这里只处理记忆存储和检索逻辑，不承载聊天 UI、Agent 主循环或模型 API 协议。

## 允许依赖

可以依赖 `common`、Room、协程、序列化和 AndroidX Core。不得依赖 UI 或 `app`。

## 禁止事项

不要在记忆日志或测试输出中复述完整个人数据、账号密码、Token 或患者/员工数据。

## 常用命令

- `./gradlew :memory:test`
- `./gradlew :memory:compileDebugKotlin`

## 验证方式

验证记忆抽取启发式、置信度变化、检索排序和上下文预算。
