## 目录职责

`app/src/androidTest` 承载整体层设备端验证，用于在真实 Android 设备或模拟器上启动完整 APK 并操作用户可见界面。

## 边界

这里只写端到端 UI 旅程和发布门禁测试，不实现业务逻辑、模型协议、Agent 循环或通用 UI 组件。

## 允许依赖

可以依赖 AndroidX Test、UIAutomator、JUnit、目标 App 包和用户可见语义。

## 禁止事项

不要在测试输出、断言消息或日志中打印 API Key、Token、Authorization 头、完整请求体或个人数据。测试应通过可见状态和脱敏错误判断结果。

## 常用命令

- `./gradlew :app:connectedDebugAndroidTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.modeltest.ProviderLightweightConnectivityTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatUserJourneyTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatMultiTurnUserJourneyTest`

## 验证方式

发布前优先运行模型轻量连通性和聊天真实旅程测试，确认当前选中模型能完成最小 ping，App 可启动、聊天输入可操作、发送按钮可用、正式聊天完成且没有空回复或误导错误。涉及模型/聊天路径时，除单轮 smoke 外还要运行多轮真实对话测试，覆盖连续输入、上下文延续、工具调用可见性、错误残留、停止状态恢复和最终自检不自报“有问题/误报/存在不一致/自相矛盾”。受限 ROM 上需要先确认 `adb shell su -c id` 可用，否则不得用该测试结果替代真实前端验证。
