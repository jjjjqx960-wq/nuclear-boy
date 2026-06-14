## 目录职责

`app.diagnostics` 属于整体层诊断编排目录，负责把 API、模型配置、第三方正式聊天兼容性、Python 执行器、文件工具和工具注册表组合成可运行的自检流程。

## 边界

这里只做跨模块诊断编排和 ADB 调试入口，不实现底层模型协议、文件系统细节或 Python 运行时。调试写配置和固定会话注入口只能作为 debug 包的运维辅助能力。

## 允许依赖

可以依赖 `api-deepseek`、`agent-core`、`python-bridge`、`tools-docgen`、`common` 和 Android/Hilt 基础能力。

## 禁止事项

不要在日志、广播结果或异常详情中输出明文 API Key、Token、授权头或完整个人数据。诊断日志只记录状态、耗时、数量和脱敏细节。

## 常用命令

- `./gradlew :app:assembleDebug`
- `adb shell am broadcast -a com.nuclearboy.app.RUN_DIAGNOSTICS -n com.nuclearboy.app.debug/com.nuclearboy.app.diagnostics.DiagnosticsReceiver`
- `adb shell am broadcast -a com.nuclearboy.app.DEBUG_SAVE_CUSTOM_MODEL -n com.nuclearboy.app.debug/com.nuclearboy.app.diagnostics.DebugModelConfigReceiver ...`

## 验证方式

验证设置页全量诊断、ADB 广播诊断、debug 模型配置写入、debug 固定会话注入、模型连通性、第三方正式聊天兼容性、Python 执行、文件写读删和工具注册表执行结果。
