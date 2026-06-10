## 目录职责

`gradle` 目录承载版本目录、依赖版本表和 Gradle wrapper 子目录。

## 边界

这里只维护构建系统配置，不放业务代码、运行时配置或密钥。

## 允许依赖

Gradle 配置可以声明插件、依赖坐标和项目版本号。

## 禁止事项

不要在这里写 Android/Kotlin 业务逻辑。不要提交本地 SDK 路径或认证信息。

## 常用命令

- `./gradlew --version`
- `./gradlew assembleDebug`

## 验证方式

修改后确认 wrapper 能解析，并至少跑一次目标构建任务。
