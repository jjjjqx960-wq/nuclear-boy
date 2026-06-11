## 目录职责

`uitest` 承载整体层 UIAutomator 用户旅程测试，验证安装后的 App 是否能按真实用户路径完成关键操作。

## 边界

这里只通过可见控件和无障碍语义操作界面，不调用内部 ViewModel、Repository 或模型客户端实现细节。

## 允许依赖

可以依赖 AndroidX Test、UIAutomator、JUnit 和目标 App 的启动入口。

## 禁止事项

不要读取或输出明文密钥、模型服务 Authorization 头、完整网络请求体或用户隐私内容。断言只使用脱敏状态、固定测试提示词和可见错误文案。

## 常用命令

- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatUserJourneyTest`

## 验证方式

测试必须能启动 App、确认目标 App 保持前台、找到聊天输入框、输入唯一消息、点击发送、等待正式聊天结束，并确认没有已知空回复、配置错误或请求格式错误。当前受限 ROM 依赖 root `su -c input` 完成真实输入事件。
