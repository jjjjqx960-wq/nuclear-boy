## 目录职责

`app/src/main/assets` 存放随 APK 打包的静态资源，包括内置技能、教程素材和离线可读资产。

## 边界

这里只放资源文件和轻量脚本，不实现 Android 入口、Hilt 装配、业务状态管理或网络协议。

## 允许依赖

资源脚本可被 `app` 模块复制、加载或通过 Python 运行时执行。资源内容不得反向依赖 Android UI 代码。

## 禁止事项

不要在资源中写入 API Key、Token、个人数据或本机绝对隐私路径。脚本输出必须避免复述明文密钥。

## 常用命令

- `./gradlew :app:assembleDebug`
- `./gradlew :app:mergeDebugAssets`

## 验证方式

修改资源后至少运行 `./gradlew :app:assembleDebug`，脚本类资源还应使用本地 Python 做一次基础语法验证。
