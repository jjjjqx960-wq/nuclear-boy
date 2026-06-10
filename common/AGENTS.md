## 目录职责

`common` 是零件层模块，负责通用常量、错误模型、结果类型、扩展函数和可复用基础能力。

## 边界

这里只放最小可复用能力，不包含业务流程、UI、网络客户端或 Android 应用入口。

## 允许依赖

可以依赖 Kotlin 标准库、协程、序列化和 AndroidX Core 等基础库。高层模块可以依赖本模块。

## 禁止事项

不要引用 `app`、`ui-*`、`agent-core`、`api-*`、`skills` 等高层模块。不要在通用错误或日志中携带明文秘密值。

## 常用命令

- `./gradlew :common:test`
- `./gradlew :common:compileDebugKotlin`

## 验证方式

验证错误映射、结果包装、字符串/文件扩展函数和时间显示逻辑。
