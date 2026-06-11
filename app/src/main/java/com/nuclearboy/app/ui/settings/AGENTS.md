## 目录职责

`app/ui/settings` 承载设置页整体 UI、状态接线和用户触发的设置流程。

## 边界

这里可以编排设置页 ViewModel、表单、测试按钮和结果展示，但可复用的展示文案、状态判断、Key 脱敏指纹和格式化规则应下沉到 `parts`。

## 允许依赖

可以依赖 `app` 装配能力、`api-deepseek` 的模型配置接口、`common` 的结果类型，以及同目录 `parts` 零件。

## 禁止事项

不要在这里实现模型协议细节、网络请求拼接、密钥明文输出或跨模块业务逻辑。

## 常用命令

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

## 验证方式

修改设置页后优先验证编译、相关单元测试、Key 指纹脱敏展示和 Debug APK 打包。
