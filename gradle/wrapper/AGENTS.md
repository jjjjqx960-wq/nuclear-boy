## 目录职责

`gradle/wrapper` 只维护 Gradle Wrapper 的 jar 与发行版属性。

## 边界

只允许修改 wrapper 版本、校验和相关属性和 wrapper jar，不放项目依赖或业务配置。

## 允许依赖

依赖 Gradle 官方发行版 URL。

## 禁止事项

不要把本机缓存路径、代理凭证或下载产物放入该目录。

## 常用命令

- `./gradlew --version`

## 验证方式

执行 wrapper 版本检查或任一 Gradle 任务确认发行版可用。
