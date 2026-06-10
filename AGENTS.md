## 目录职责

仓库根目录承载 Android 多模块工程的整体编排、Gradle 入口、仓库级说明和发布版本信息。

## 边界

根目录只放整体级文件，例如 `settings.gradle.kts`、根 `build.gradle.kts`、`README.md`、仓库级交接文档和版本记录。具体实现必须下沉到各模块。

## 允许依赖

整体层可以引用各模块；模块之间依赖以 `settings.gradle.kts` 和各模块 `build.gradle.kts` 为准。

## 禁止事项

不要在根目录新增业务实现代码。不要在根目录存放 API Key、Token、签名密钥、个人数据或构建产物。

## 常用命令

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew clean assembleDebug`

## 验证方式

优先验证 `./gradlew assembleDebug`。涉及单元逻辑时补跑对应模块测试。
