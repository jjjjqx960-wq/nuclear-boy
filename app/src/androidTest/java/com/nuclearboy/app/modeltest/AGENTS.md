## 目录职责

`modeltest` 承载整体层设备端模型连通性测试，使用安装后的 App 配置验证当前选中模型的轻量请求链路。

## 边界

这里只做发布门槛级设备端模型连通性验证，不实现 UI 旅程、模型协议细节或 Agent 编排。

## 允许依赖

可以依赖 AndroidX Test、JUnit、`api-deepseek` 的公开配置和诊断 API，以及目标 App 的安全配置存储。

## 禁止事项

不要在断言、日志或命令输出中打印 API Key、Authorization 头、完整请求体或用户隐私内容。失败信息只能包含脱敏端点、模型名、错误类型和短诊断片段。

## 常用命令

- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.modeltest.ProviderLightweightConnectivityTest`

## 验证方式

发布前涉及模型配置或聊天链路时，先运行轻量连通性测试确认当前选中自定义模型能完成最小 ping，再运行 `uitest` 的正式聊天旅程，不能用轻量 ping 代替前端正式聊天。
