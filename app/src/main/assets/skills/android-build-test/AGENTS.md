## 目录职责

`android-build-test` 是内置技能，负责为核弹男孩 Android 工程生成和执行受控的编译、测试、ADB 自检流程。

## 边界

技能只编排 Gradle、ADB 和项目检查，不修改业务代码、不保存密钥、不上传结果。

## 允许依赖

允许读取工作区文件，允许在显式 `execute=true` 时执行白名单命令：Gradle Wrapper、`adb`。

## 禁止事项

不要拼接任意 shell 字符串，不要执行用户传入的任意命令，不要输出 API Key、Token、Authorization 头或完整隐私数据。

## 常用命令

- `python main.py`
- `python main.py all --execute`
- `./gradlew :app:assembleDebug`

## 验证方式

本地运行 `python app/src/main/assets/skills/android-build-test/main.py doctor`，再运行 `./gradlew :app:assembleDebug`。
