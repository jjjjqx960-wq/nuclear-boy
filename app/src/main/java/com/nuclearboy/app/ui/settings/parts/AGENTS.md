## 目录职责

`settings/parts` 是设置页零件层，放置最小可复用的展示文案、状态判断、Key 脱敏指纹、模型名清理提示、模型测试/全量自检复制摘要和格式化规则。

## 边界

这里只放纯函数和轻量规则，不持有 Compose 状态、不发起网络请求、不读写本地配置。

## 允许依赖

可以依赖 `common` 的基础类型。不得依赖设置页 ViewModel、Compose 组件或高层模块。

## 禁止事项

不要保存或输出明文 API Key、Token、签名密钥或个人数据；只允许输出不可逆摘要、长度等脱敏核对信息。不要在零件层调用 Android UI 或网络 API。

## 常用命令

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`

## 验证方式

新增规则必须补单元测试，覆盖关键分支、脱敏输出、模型名清理提示、模型测试/全量自检复制摘要和兜底文案。
