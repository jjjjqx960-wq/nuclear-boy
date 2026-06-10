# NUCLEAR BOY (核弹男孩)

> 温暖、智能、人性化的移动端 AI 编程助手 · v1.0.2

---

## ⚠️ 声明

**本仓库非原作者上传。**

- **原作者**: [mzpr00](https://github.com/mzpr00) (穆再排尔·穆合塔尔)
- **本仓库维护者**: [jjjjqx960-wq](https://github.com/jjjjqx960-wq)
- **说明**: 本仓库为原项目的复制/镜像（mirror），仅供学习研究使用。所有代码版权归原作者 mzpr00 所有。

---

## 项目简介

Nuclear Boy 是一个基于 Android 的移动端 AI 编程助手，内置 Agent 引擎、工具注册、Python 执行器、三层记忆系统等能力。

## 技术栈

- **UI**: Jetpack Compose + Material 3 (暖色主题 #FF8C42)
- **DI**: Hilt (SingletonComponent)
- **网络**: OkHttp 4 + 流式 SSE
- **数据库**: Room (SQLite)
- **Python**: Chaquopy 15 (嵌入式 CPython)
- **序列化**: kotlinx.serialization

## 构建

1. 安装 Android Studio + SDK 35
2. 设置 `ANDROID_HOME` 环境变量
3. `./gradlew assembleDebug`

## 1.0.2 重点

- 修复第三方 OpenAI 兼容服务的地址归一化与无鉴权网关支持。
- 第三方模型支持列表化管理，可新增、点选、删除，并可在主聊天页左上角快速切换。
- 设置页新增第三方模型测试诊断，可提示鉴权、路径、模型名、网络超时和 HTTP 私有网关问题。
- 修复流式工具调用的多工具 index 累积问题。
- 移除 API 客户端默认信任所有 TLS 证书的高风险行为。
- 持久化 `/goal` 会话目标，并修复 `/loop` 运行中无法稳定 `/stop` 的问题。
- Python 工具执行链调整为非隔离执行，设置页与命令帮助中移除沙箱开关。
- 对齐 Gradle wrapper 到本地已验证的 8.14.4。

## 许可证

原始项目版权归原作者 mzpr00 所有。如有任何问题请联系原作者或本仓库维护者。
