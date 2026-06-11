## 目录职责

`app` 是整体层模块，负责 Android 应用入口、Hilt 装配、导航、主题、前台服务、更新流程和跨模块协调。

## 边界

这里可以装配下层模块，但不应实现模型协议、Agent 核心循环、文件工具细节或通用 UI 组件逻辑。

## 允许依赖

可以依赖所有业务模块：`common`、`api-deepseek`、`agent-core`、`python-bridge`、`memory`、`skills`、`tools-docgen`、`ui-chat`、`ui-workspace`。

## 禁止事项

不要在装配日志中输出完整工具参数、明文密钥或个人数据。新增跨模块能力时先在低层模块提供稳定接口。

## 常用命令

- `./gradlew :app:assembleDebug`
- `./gradlew :app:compileDebugKotlin`

## 验证方式

重点验证 Hilt 装配、APK 打包、权限声明、设置页流程、运行时工具注册和 `app/src/androidTest` 真实 UI 旅程门禁。
