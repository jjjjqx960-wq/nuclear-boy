## 目录职责

`ui-chat` 是部件层模块，负责聊天界面、聊天 ViewModel、斜杠命令、消息渲染、Token HUD 和聊天侧工作区交互，包括文件浏览、过滤、摘要、排序、选择、预览和引用入口。

## 边界

这里组合 Agent、模型、技能和文件组件形成用户聊天工作流，不直接实现底层模型协议或文件系统细节。

## 允许依赖

可以依赖 `common`、`api-deepseek`、`agent-core`、`skills`、`tools-docgen` 和 Compose UI 库。

## 禁止事项

不要在聊天日志或系统消息里输出明文秘密值。不要把持久化文件格式散落到多个 UI 组件中。

## 常用命令

- `./gradlew :ui-chat:test`
- `./gradlew :ui-chat:compileDebugKotlin`

## 验证方式

验证普通发送、`/goal`、`/loop`、`/stop`、`/compact`、`/rewind`、消息持久化、流式状态更新，以及文件面板浏览、过滤、摘要、排序、选择、预览和引用链路。
