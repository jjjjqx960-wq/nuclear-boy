# 核弹男孩项目树状结构

```text
nuclear-boy-main/
├── AGENTS.md                       # 仓库级协作规则与分层约束
├── CHANGELOG.md                    # 仓库级 1.0.2 更新记录
├── build.gradle.kts                # 根 Gradle 配置
├── settings.gradle.kts             # Android 多模块注册入口
├── gradle/                         # Gradle Wrapper 与版本目录
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   ├── libs.versions.toml
│   └── wrapper/
├── app/                            # 整体层：Android App 入口、Hilt 装配、页面、服务、诊断
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── AGENTS.md
│       │   ├── CHANGELOG.md
│       │   └── skills/
│       │       ├── AGENTS.md
│       │       ├── CHANGELOG.md
│       │       ├── android-build-test/    # Android 编译/测试/ADB 诊断技能
│       │       ├── android_bridge/
│       │       ├── code-formatter/
│       │       ├── code-reviewer/
│       │       ├── daily-briefing/
│       │       ├── file-organizer/
│       │       └── skill-creator/
│       ├── java/com/nuclearboy/app/
│       │   ├── di/                 # Hilt Provider，连接各模块
│       │   ├── diagnostics/        # 全量自检与 ADB Debug 广播
│       │   ├── python/             # Chaquopy Python 执行器
│       │   ├── service/            # 前台服务
│       │   ├── ui/settings/        # API Key、第三方模型、协议、诊断设置页
│       │   └── update/             # 更新检查与下载
│       └── res/                    # Android 资源、网络安全配置、FileProvider 配置
├── common/                         # 零件层：通用类型、错误、常量、模型枚举、扩展函数
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   └── src/main/java/com/nuclearboy/common/
├── api-deepseek/                   # 组件层：DeepSeek/OpenAI/Anthropic 兼容 API、模型配置、Token 统计
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   └── src/main/java/com/nuclearboy/api/deepseek/
│       ├── ApiKeyManager.kt        # 加密配置、模型列表、协议字段、activeModelId
│       ├── DeepSeekApiClient.kt    # OpenAI/Anthropic 请求、SSE、诊断测试
│       ├── ProviderProtocol.kt     # 自动/OpenAI/Anthropic 协议选择
│       ├── DeepSeekModels.kt       # DTO
│       ├── ModelRouter.kt          # 模型路由
│       ├── ContextWindowManager.kt # 上下文预算
│       └── TokenTracker.kt         # Token/费用统计
├── agent-core/                     # 部件层：Agent 循环、工具注册、系统提示、上下文组装
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   └── src/main/java/com/nuclearboy/agent/
├── python-bridge/                  # 组件层：PythonSandbox、PythonExecutor 与执行结果封装
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   └── src/main/java/com/nuclearboy/python/
├── skills/                         # 组件层：技能 manifest、安装、解析、执行、市场
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   └── src/main/java/com/nuclearboy/skills/
├── tools-docgen/                   # 组件层：文件操作与文档生成工具
├── memory/                         # 组件层：记忆存储
├── ui-chat/                        # 部件层：聊天主界面、模型切换、消息流渲染
│   ├── AGENTS.md
│   ├── CHANGELOG.md
│   └── src/main/java/com/nuclearboy/ui/chat/
├── ui-workspace/                   # 部件层：工作区相关 UI
├── android-bridge-v1.0/            # Android 桥接参考/历史资源
└── llmwiki/                        # LLM 可读项目知识索引
    ├── AGENTS.md
    ├── CHANGELOG.md
    ├── INDEX.md
    └── PROJECT_TREE.md
```

## 关键链路

```text
聊天请求:
ui-chat/ChatScreen
  -> ui-chat/ChatViewModel
  -> agent-core/AgentEngine
  -> api-deepseek/DeepSeekApiClient
  -> agent-core/ToolRegistry
  -> tools-docgen/FileOperations | python-bridge/PythonSandbox | skills/SkillManager

第三方模型配置:
app/ui/settings/SettingsScreen
  -> api-deepseek/ApiKeyManager
  -> api-deepseek/ProviderProtocol
  -> api-deepseek/DeepSeekApiClient

功能自检:
app/ui/settings/SettingsScreen
  -> app/diagnostics/AppDiagnostics
  -> api-deepseek + python-bridge + tools-docgen + agent-core
  -> app/diagnostics/DiagnosticsReceiver for ADB broadcast

内置技能:
app/src/main/assets/skills/<skill-name>/skill.yaml
  -> skills/SkillManager
  -> python-bridge/PythonSandbox
  -> agent-core/ToolRegistry as skill_<name>
```

## 推荐读取顺序

1. `AGENTS.md`
2. `llmwiki/INDEX.md`
3. 当前任务相关模块的 `AGENTS.md`
4. 当前任务相关源码文件
5. 对应模块 `CHANGELOG.md`

## 编译测试入口

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat :api-deepseek:testDebugUnitTest --console=plain
adb shell am broadcast -a com.nuclearboy.app.RUN_DIAGNOSTICS -n com.nuclearboy.app.debug/com.nuclearboy.app.diagnostics.DiagnosticsReceiver
```
