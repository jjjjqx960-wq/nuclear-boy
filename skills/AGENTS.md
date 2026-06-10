## 目录职责

`skills` 是组件层模块，负责技能清单、参数校验、权限模型、技能安装和技能执行相关能力。

## 边界

这里只处理技能定义与执行，不承载应用入口、聊天页面或模型 API 请求。

## 允许依赖

可以依赖 `common`、`python-bridge`、协程、序列化和 AndroidX Core。不得依赖 UI 或 `app`。

## 禁止事项

不要让技能权限默认越权。不要在技能日志或清单中写入明文密钥、Token、账号密码或完整个人数据。

## 常用命令

- `./gradlew :skills:test`
- `./gradlew :skills:compileDebugKotlin`

## 验证方式

验证技能参数类型、权限限制判定、文件 glob 匹配和技能清单解析。
