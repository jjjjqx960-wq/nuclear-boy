## 目录职责

`agent-core` 的 JVM 单元测试目录，验证 Agent 编排、工具注册、系统提示、工具调用保护和重试错误门控等纯逻辑。

## 边界

这里只写不依赖 Android 设备的快速单元测试。真实 App 启动、聊天 UI 和模型链路放在 `app/src/androidTest`。

## 允许依赖

可以依赖 JUnit、`agent-core` 主代码和上游 DTO 类型。

## 禁止事项

不要在测试断言或输出中写入真实 API Key、Token、授权头、个人数据或完整用户私密内容。

## 常用命令

- `./gradlew :agent-core:test`

## 验证方式

新增 Agent 纯逻辑后补充对应单测，确保失败信息能定位到具体保护条件或编排行为。
