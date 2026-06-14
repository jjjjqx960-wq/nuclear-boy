## 目录职责

`uitest` 承载整体层 UIAutomator 用户旅程测试，验证安装后的 App 是否能按真实用户路径完成关键操作。

## 边界

这里只通过可见控件和无障碍语义操作界面，不调用内部 ViewModel、Repository 或模型客户端实现细节。可抽取 UIAutomator robot 复用启动、输入、发送、等待和错误拦截等真实前端操作。

## 允许依赖

可以依赖 AndroidX Test、UIAutomator、JUnit 和目标 App 的启动入口。

## 禁止事项

不要读取或输出明文密钥、模型服务 Authorization 头、完整网络请求体或用户隐私内容。断言只使用脱敏状态、固定测试提示词和可见错误文案。

## 常用命令

- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatUserJourneyTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatMultiTurnUserJourneyTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.SkillPromptGenerationJourneyTest`

## 验证方式

测试必须能启动 App、确认目标 App 保持前台、找到聊天输入框、输入唯一消息、点击发送、等待正式聊天结束，并确认没有已知空回复、配置错误或请求格式错误。多轮对话测试还必须连续发送多类提示，覆盖上下文延续、工具调用、输入清空、停止状态恢复、错误残留，以及最终轮不自报“上下文有问题/工具结果有问题/存在问题/误报/存在不一致/自相矛盾”等语义失败；断言词必须避免误伤用户提示里的“没有问题”。skill/prompt 生成测试必须通过前端对话触发文件工具，并从设备 workspace 目录读取 marker 证明真实落盘；若第三方网关降级导致后续工具不可用，测试必须要求界面明确说明工具受限、未真实写入，不能接受文本模拟的假成功。当前受限 ROM 依赖 root `su -c input` 完成真实点击；中文长文本可通过 UIAutomator 对可见输入框执行文本设置。
