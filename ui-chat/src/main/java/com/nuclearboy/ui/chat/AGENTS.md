## 目录职责

`chat` 是 `ui-chat` 的部件层源码目录，承载聊天屏幕、聊天 ViewModel、消息渲染、输入区、Token HUD 和文件面板等用户聊天工作流。

## 边界

这里可以组合组件层与下游业务模块形成完整聊天体验，但新增可复用数据和局部 UI 应先下沉到 `parts` 或 `components`。

## 允许依赖

允许依赖 `components`、`parts`、`common`、`api-deepseek`、`agent-core`、`skills`、`tools-docgen`、`memory` 和 Compose/Hilt 相关库。

## 禁止事项

不要在 UI 文案、日志或系统消息中复述明文 Token、密码、签名密钥或完整个人数据。不要把模型协议、文件系统细节或工具执行逻辑写进屏幕组件。

## 常用命令

- `./gradlew :ui-chat:compileDebugKotlin`
- `./gradlew :app:assembleDebug`

## 验证方式

重点验证普通发送、快捷命令、`/goal`、`/loop`、`/stop`、`/compact`、`/rewind`、消息持久化、流式状态更新，以及文件面板过滤、摘要、只看已选过滤摘要、只看已选列表过滤、只看已选匹配状态、取消匹配选择、引用匹配、引用按钮数量标签、匹配引用后保留未引用选择、匹配引用后过滤复位、批量引用剩余数量提示、最后一批匹配引用自动收起、排序、单选引用、批量引用、隐藏选择清理、无匹配隐藏选择操作条、预览和引用文件。
