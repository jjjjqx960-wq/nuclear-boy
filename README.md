# NUCLEAR BOY (核弹男孩)

> 温暖、智能、人性化的移动端 AI 编程助手

---

## ⚠️ 声明

**本仓库非原作者上传。**

- **原作者**: [mzpr00](https://github.com/mzpr00) (穆再排尔·穆合塔尔)
- **本仓库维护者**: [jjjjqx960-wq](https://github.com/jjjjqx960-wq)
- **说明**: 本仓库为原项目的复制/镜像（mirror），仅供学习研究使用。所有代码版权归原作者 mzpr00 所有。

---

## 项目简介

Nuclear Boy 是一个基于 Android 的移动端 AI 编程助手，内置 Agent 引擎、工具注册、Python 沙箱、三层记忆系统等能力。

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

## 许可证

原始项目版权归原作者 mzpr00 所有。如有任何问题请联系原作者或本仓库维护者。
