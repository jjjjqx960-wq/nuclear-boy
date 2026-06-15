## 目录职责

`uitest` 承载整体层 UIAutomator 用户旅程测试，验证安装后的 App 是否能按真实用户路径完成关键操作。

## 边界

这里只通过可见控件和无障碍语义操作界面，不调用内部 ViewModel、Repository 或模型客户端实现细节。允许通过 debug-only Receiver 让目标 App 进程写入固定测试会话来验证纯前端渲染状态，但不得绕过真实聊天链路门禁。可抽取 UIAutomator robot 复用启动、输入、发送、等待和错误拦截等真实前端操作。

## 允许依赖

可以依赖 AndroidX Test、UIAutomator、JUnit 和目标 App 的启动入口。

## 禁止事项

不要读取或输出明文密钥、模型服务 Authorization 头、完整网络请求体或用户隐私内容。断言只使用脱敏状态、固定测试提示词和可见错误文案。不要从 test 进程、宿主 shell 或 `run-as` 直接写 `/storage/emulated/0/Android/data/<package>`；受限 ROM 可能拦截这些路径，固定 workspace 状态应交给目标 App 的 debug-only 入口完成。

## 常用命令

- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatUserJourneyTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.ChatMultiTurnUserJourneyTest`
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nuclearboy.app.uitest.SkillPromptGenerationJourneyTest`

## 验证方式

测试必须能启动 App、处理常见系统权限弹窗、确认目标 App 保持前台、找到聊天输入框、输入唯一消息、点击发送、等待正式聊天结束，并确认没有已知空回复、配置错误或请求格式错误。多轮对话测试还必须连续发送多类提示，覆盖上下文延续、工具调用、输入清空、停止状态恢复、错误残留，以及最终轮不自报“上下文有问题/工具结果有问题/存在问题/误报/存在不一致/自相矛盾”等语义失败；断言词必须避免误伤用户提示里的“没有问题”。skill/prompt 生成测试必须通过前端对话触发文件工具，并从设备 workspace 目录读取 marker 证明真实落盘；若第三方网关降级导致任一写文件步骤工具不可用，测试必须要求界面明确说明工具受限、未真实写入，不能接受文本模拟的假成功。工具受限提示卡和正式聊天失败诊断卡测试可以通过 debug-only Receiver 写入固定会话，但必须启动真实 App 并验证可见文本、脱敏诊断指纹、正式链路测试口径和无障碍语义；工具受限卡需要额外验证 `tool.protocol` 诊断指纹；诊断卡新增错误分类、指纹字段或测试口径时至少覆盖一个真机种子会话，优先验证用户最常见的鉴权、模型路由或限流问题。工具型草稿预警测试可以写入本地假自定义模型配置，但必须通过 `ChatJourneyRobot.enterDraftText()` 复用真实聚焦、清空、设值、草稿持有、真实点击发送和发送后证据提示确认路径，不要直接裸用 `UiObject2.setText` 后断言发送前 UI。工具型请求后置复核测试可使用不可达调试模型触发无工具证据完成态，并通过真实输入、发送、等待、可见标题、可见下一步、`tool.evidence.missing` 诊断指纹、正式链路测试口径和警告卡无障碍语义确认 UI 追加结构化复核提示；正式聊天可用性仍必须由真实网关 `stream=true` 门禁覆盖。Instrumentation 参数中的调试模型配置和调试会话内容必须通过目标 App debug-only Receiver 注入，并用 base64 extras 传递 baseUrl、模型名、Key 和长文案，避免 `http://`、空格或特殊字符被 shell 拆坏。当前受限 ROM 依赖 root `su -c input` 完成真实点击；中文长文本可通过 UIAutomator 对可见输入框执行文本设置。
