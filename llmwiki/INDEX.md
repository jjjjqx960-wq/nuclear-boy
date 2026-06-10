# 核弹男孩 LLMwiki

本目录给 LLM/Agent 快速读取项目结构、模块边界和验证方式。优先读本文件，再按任务需要打开树状结构。

## 快速入口

- 项目树：`llmwiki/PROJECT_TREE.md`
- 根目录规则：`AGENTS.md`
- 应用层规则：`app/AGENTS.md`
- 模型协议层：`api-deepseek/AGENTS.md`
- Agent 核心层：`agent-core/AGENTS.md`
- Python 桥接层：`python-bridge/AGENTS.md`
- 内置技能：`app/src/main/assets/skills/AGENTS.md`

## 常用验证

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :api-deepseek:testDebugUnitTest --console=plain
.\gradlew.bat test --console=plain
adb shell am broadcast -a com.nuclearboy.app.RUN_DIAGNOSTICS -n com.nuclearboy.app.debug/com.nuclearboy.app.diagnostics.DiagnosticsReceiver
```

## 维护原则

- 改代码前先读对应模块 `AGENTS.md`。
- 新增目录时同步补 `AGENTS.md` 和 `CHANGELOG.md`。
- 涉及 Key、Token、个人数据时只写脱敏摘要。
- Android 编译测试优先使用内置技能 `android-build-test` 生成命令计划。
