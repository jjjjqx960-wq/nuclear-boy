## 目录职责

`parts` 是 `ui-chat` 的零件层，承载聊天 UI 可复用的最小数据结构、常量和纯函数，例如命令元数据、滚动状态判断、文件面板过滤规则和文件面板摘要计算。

## 边界

这里只描述稳定的小能力，不组合 Compose 布局，不依赖 ViewModel、Agent、文件系统或网络。

## 允许依赖

允许依赖 Kotlin 标准库和同层纯数据类型。

## 禁止事项

不要在零件层读取 Android Context、持久化文件、调用模型接口或直接渲染界面。

## 常用命令

- `./gradlew :ui-chat:compileDebugKotlin`
- `./gradlew :ui-chat:testDebugUnitTest`

## 验证方式

优先验证纯函数输入输出，包括空输入、大小写、扩展名、路径匹配、目录/文件计数和类型分布，再由组件层和部件层编译验证接入效果。
